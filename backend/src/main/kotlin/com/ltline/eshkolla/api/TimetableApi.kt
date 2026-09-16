package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class TimetableSettingsDto(val id:String,val schoolId:String?=null,val firstLessonStart:String,val lessonDurationMinutes:Int,val shortBreakMinutes:Int,val longBreakAfterLesson:Int,val longBreakMinutes:Int,val lessonsPerDay:Int,val active:Boolean=true)
@Serializable
data class TimetableSettingsRequest(val schoolId:String?=null,val firstLessonStart:String,val lessonDurationMinutes:Int,val shortBreakMinutes:Int,val longBreakAfterLesson:Int,val longBreakMinutes:Int,val lessonsPerDay:Int,val active:Boolean=true)
@Serializable
data class LessonPeriodDto(val lessonNumber:Int,val startTime:String,val endTime:String,val breakAfterMinutes:Int,val breakTypeAfter:String)
@Serializable
data class TimetableDto(val settings:TimetableSettingsDto,val periods:List<LessonPeriodDto>,val schedule:List<ScheduleDto>)

fun Application.configureTimetableApi(authService: AuthService) {
    routing {
        route("/api/v1") {
            get("/timetable") {
                val user=requireTimetableUser(call,authService)?:return@get
                val settings=loadSettings(call.request.queryParameters["schoolId"])
                call.respond(TimetableDto(settings,buildPeriods(settings),loadScopedSchedule(user,call.request.queryParameters["classId"])))
            }
            get("/management/timetable-settings") {
                val user=requireTimetableUser(call,authService)?:return@get
                if(user.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë të menaxhoni orarin e shkollës."));return@get}
                call.respond(loadSettings(call.request.queryParameters["schoolId"]))
            }
            put("/management/timetable-settings") {
                val user=requireTimetableUser(call,authService)?:return@put
                if(user.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni të drejtë të ndryshoni orarin e shkollës."));return@put}
                val request=call.receive<TimetableSettingsRequest>()
                if(!validSettings(request)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Parametrat e orarit nuk janë valide."));return@put}
                val school=request.schoolId?.trim()?.takeIf{it.isNotBlank()}
                val id=school?.let{"SCHOOL-$it"}?:"DEFAULT"
                Database.connection().use { c ->
                    val updated=c.prepareStatement("UPDATE school_timetable_settings SET school_id=?,first_lesson_start=?,lesson_duration_minutes=?,short_break_minutes=?,long_break_after_lesson=?,long_break_minutes=?,lessons_per_day=?,active=? WHERE id=?").use { p ->
                        p.setString(1,school);p.setString(2,request.firstLessonStart);p.setInt(3,request.lessonDurationMinutes);p.setInt(4,request.shortBreakMinutes);p.setInt(5,request.longBreakAfterLesson);p.setInt(6,request.longBreakMinutes);p.setInt(7,request.lessonsPerDay);p.setBoolean(8,request.active);p.setString(9,id);p.executeUpdate()
                    }
                    if(updated==0)c.prepareStatement("INSERT INTO school_timetable_settings(id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active) VALUES(?,?,?,?,?,?,?,?,?)").use { p ->
                        p.setString(1,id);p.setString(2,school);p.setString(3,request.firstLessonStart);p.setInt(4,request.lessonDurationMinutes);p.setInt(5,request.shortBreakMinutes);p.setInt(6,request.longBreakAfterLesson);p.setInt(7,request.longBreakMinutes);p.setInt(8,request.lessonsPerDay);p.setBoolean(9,request.active);p.executeUpdate()
                    }
                }
                call.respond(loadSettings(school))
            }
        }
    }
}

private suspend fun requireTimetableUser(call:ApplicationCall,auth:AuthService):UserDto?{val token=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()};if(token==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return null};val user=auth.userFor(token);if(user==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Sesioni nuk është i vlefshëm."));return null};return user}

private fun loadSettings(schoolId:String?):TimetableSettingsDto=Database.connection().use{c->
    val s=schoolId?.trim()?.takeIf{it.isNotBlank()};val sql=if(s==null)"SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE id='DEFAULT' LIMIT 1" else "SELECT id,school_id,first_lesson_start,lesson_duration_minutes,short_break_minutes,long_break_after_lesson,long_break_minutes,lessons_per_day,active FROM school_timetable_settings WHERE school_id=? AND active=TRUE LIMIT 1"
    c.prepareStatement(sql).use{p->{if(s!=null)p.setString(1,s);p.executeQuery().use{r->{if(!r.next())return@use TimetableSettingsDto("DEFAULT",null,"08:00",45,5,4,30,6,true);TimetableSettingsDto(r.getString("id"),r.getString("school_id"),r.getString("first_lesson_start"),r.getInt("lesson_duration_minutes"),r.getInt("short_break_minutes"),r.getInt("long_break_after_lesson"),r.getInt("long_break_minutes"),r.getInt("lessons_per_day"),r.getBoolean("active"))}}}}
}

private fun buildPeriods(s:TimetableSettingsDto):List<LessonPeriodDto>{val f=DateTimeFormatter.ofPattern("HH:mm");var cursor=LocalTime.parse(s.firstLessonStart,f);return(1..s.lessonsPerDay).map{n->val start=cursor;val end=start.plusMinutes(s.lessonDurationMinutes.toLong());val long=n==s.longBreakAfterLesson&&n<s.lessonsPerDay;val mins=if(n==s.lessonsPerDay)0 else if(long)s.longBreakMinutes else s.shortBreakMinutes;cursor=end.plusMinutes(mins.toLong());LessonPeriodDto(n,start.format(f),end.format(f),mins,if(mins==0)"NONE" else if(long)"PUSHIM_I_GJATE" else "PAUZE")}}
private fun validSettings(r:TimetableSettingsRequest)=timeRegex.matches(r.firstLessonStart)&&r.lessonDurationMinutes in 20..120&&r.shortBreakMinutes in 0..30&&r.longBreakMinutes in 0..90&&r.lessonsPerDay in 1..12&&r.longBreakAfterLesson in 1..r.lessonsPerDay
private val timeRegex=Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun loadScopedSchedule(user:UserDto,classId:String?):List<ScheduleDto>=Database.connection().use{c->{
    val requested=classId?.trim()?.takeIf{it.isNotBlank()};val sql:String;val params=mutableListOf<String>()
    when(user.role){
        "MESIMDHENES"->{sql=if(requested==null)"SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE ORDER BY s.weekday,s.start_time" else "SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s JOIN teachers t ON t.id=s.teacher_id WHERE t.user_id=? AND t.active=TRUE AND s.active=TRUE AND s.class_id=? ORDER BY s.weekday,s.start_time";params+=user.id;if(requested!=null)params+=requested}
        "NXENES"->{val sid=findStudentId(user.id)?:return@use emptyList();val cid=findStudentClass(sid)?:return@use emptyList();sql="SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE class_id=? AND active=TRUE ORDER BY weekday,start_time";params+=cid}
        "PRIND"->{sql="SELECT s.id,s.class_id,s.teacher_id,s.subject_id,s.weekday,s.start_time,s.end_time,s.room,s.active FROM schedules s WHERE s.class_id IN (SELECT st.class_id FROM students st JOIN parent_students p ON p.student_id=st.id WHERE p.user_id=? AND st.active=TRUE) AND s.active=TRUE ORDER BY s.weekday,s.start_time";params+=user.id}
        else->{sql=if(requested==null)"SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE ORDER BY weekday,start_time" else "SELECT id,class_id,teacher_id,subject_id,weekday,start_time,end_time,room,active FROM schedules WHERE active=TRUE AND class_id=? ORDER BY weekday,start_time";if(requested!=null)params+=requested}
    }
    c.prepareStatement(sql).use{p->{params.forEachIndexed{index,value->p.setString(index+1,value)};p.executeQuery().use{r->buildList{while(r.next())add(ScheduleDto(r.getString("id"),r.getString("class_id"),r.getString("teacher_id"),r.getString("subject_id"),r.getInt("weekday"),r.getString("start_time"),r.getString("end_time"),r.getString("room"),r.getBoolean("active")))}}}}
}}
private fun findStudentId(userId:String):String?=Database.connection().use{c->c.prepareStatement("SELECT student_id FROM student_users WHERE user_id=? LIMIT 1").use{p->p.setString(1,userId);p.executeQuery().use{r->if(r.next())r.getString(1)else null}}}
private fun findStudentClass(studentId:String):String?=Database.connection().use{c->c.prepareStatement("SELECT class_id FROM students WHERE id=? LIMIT 1").use{p->p.setString(1,studentId);p.executeQuery().use{r->if(r.next())r.getString(1)else null}}}
