package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class TeacherScheduleCodeDto(val code:Int,val teacherId:String?=null,val displayName:String?=null,val subjectId:String?=null,val subjectName:String?=null,val classIds:List<String> = emptyList(),val classNames:List<String> = emptyList(),val active:Boolean=true)
@Serializable
data class TeacherScheduleCodeRequest(val code:Int,val teacherId:String?=null,val displayName:String?=null,val subjectId:String?=null,val classIds:List<String> = emptyList(),val active:Boolean=true)

fun Application.configureTeacherScheduleCodeApi(authService:AuthService){
 routing{route("/api/v1/management/timetable-codes"){
  get{if(requireManager(call,authService)==null)return@get;call.respond(loadCodes())}
  post{if(requireManager(call,authService)==null)return@post;val r=call.receive<TeacherScheduleCodeRequest>();val e=validate(r);if(e!=null){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR",e));return@post};call.respond(HttpStatusCode.Created,saveCode(r,true))}
  put("/{code}"){if(requireManager(call,authService)==null)return@put;val code=call.parameters["code"]?.toIntOrNull();if(code==null||code !in 1..17){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Kodi duhet të jetë nga 1 deri në 17."));return@put};val r=call.receive<TeacherScheduleCodeRequest>().copy(code=code);val e=validate(r);if(e!=null){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR",e));return@put};if(!codeExists(code)){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Kodi nuk ekziston."));return@put};call.respond(saveCode(r,false))}
  delete("/{code}"){if(requireManager(call,authService)==null)return@delete;val code=call.parameters["code"]?.toIntOrNull();if(code==null||code !in 1..17){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Kodi duhet të jetë nga 1 deri në 17."));return@delete};val n=Database.connection().use{c->c.autoCommit=false;try{c.prepareStatement("DELETE FROM teacher_schedule_code_classes WHERE code=?").use{p->p.setInt(1,code);p.executeUpdate()};val x=c.prepareStatement("UPDATE teacher_schedule_codes SET teacher_id=NULL,display_name=NULL,subject_id=NULL,active=TRUE WHERE code=?").use{p->p.setInt(1,code);p.executeUpdate()};c.commit();x}catch(e:Exception){c.rollback();throw e}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Kodi nuk ekziston."))else call.respond(loadCodes().first{it.code==code})}
 }}
}

private suspend fun requireManager(call:io.ktor.server.application.ApplicationCall,auth:AuthService):UserDto?{val token=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim().orEmpty();if(token.isBlank()){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return null};val u=auth.userFor(token);if(u==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Sesioni nuk është i vlefshëm."));return null};if(u.role !in setOf("ADMINISTRATOR","DREJTOR")){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori ose drejtori mund të menaxhojë kodet e orarit."));return null};return u}
private fun validate(r:TeacherScheduleCodeRequest):String?{if(r.code !in 1..17)return "Kodi duhet të jetë nga 1 deri në 17.";if(r.displayName.orEmpty().length>200)return "Emri i mësimdhënësit është shumë i gjatë.";if(r.classIds.distinct().size!=r.classIds.size)return "Një klasë/paralele është përsëritur.";return null}
private fun codeExists(code:Int)=Database.connection().use{c->c.prepareStatement("SELECT 1 FROM teacher_schedule_codes WHERE code=?").use{p->p.setInt(1,code);p.executeQuery().use{it.next()}}}
private fun saveCode(r:TeacherScheduleCodeRequest,allowCreate:Boolean):TeacherScheduleCodeDto{Database.connection().use{c->c.autoCommit=false;try{if(!allowCreate&&!codeExistsIn(c,r.code))error("Kodi nuk ekziston.");c.prepareStatement("INSERT INTO teacher_schedule_codes(code,teacher_id,display_name,subject_id,active) VALUES(?,?,?,?,?) ON CONFLICT(code) DO UPDATE SET teacher_id=EXCLUDED.teacher_id,display_name=EXCLUDED.display_name,subject_id=EXCLUDED.subject_id,active=EXCLUDED.active").use{p->p.setInt(1,r.code);p.setString(2,r.teacherId?.trim()?.takeIf{it.isNotBlank()});p.setString(3,r.displayName?.trim()?.takeIf{it.isNotBlank()});p.setString(4,r.subjectId?.trim()?.takeIf{it.isNotBlank()});p.setBoolean(5,r.active);p.executeUpdate()};c.prepareStatement("DELETE FROM teacher_schedule_code_classes WHERE code=?").use{p->p.setInt(1,r.code);p.executeUpdate()};r.classIds.map{it.trim()}.filter{it.isNotBlank()}.distinct().forEach{classId->c.prepareStatement("INSERT INTO teacher_schedule_code_classes(code,class_id) VALUES(?,?) ON CONFLICT DO NOTHING").use{p->p.setInt(1,r.code);p.setString(2,classId);p.executeUpdate()}};c.commit()}catch(e:Exception){c.rollback();throw e}};return loadCodes().first{it.code==r.code}}
private fun codeExistsIn(c:java.sql.Connection,code:Int)=c.prepareStatement("SELECT 1 FROM teacher_schedule_codes WHERE code=?").use{p->p.setInt(1,code);p.executeQuery().use{it.next()}}
private fun loadCodes():List<TeacherScheduleCodeDto>=Database.connection().use{c->{val rows=c.prepareStatement("SELECT t.code,t.teacher_id,t.display_name,t.subject_id,s.name AS subject_name,t.active FROM teacher_schedule_codes t LEFT JOIN subjects s ON s.id=t.subject_id ORDER BY t.code").use{p->p.executeQuery().use{rs->buildList{while(rs.next())add(TeacherScheduleCodeDto(rs.getInt("code"),rs.getString("teacher_id"),rs.getString("display_name"),rs.getString("subject_id"),rs.getString("subject_name"),active=rs.getBoolean("active")))}}}};rows.map{row->val cls=c.prepareStatement("SELECT tc.class_id,c.name FROM teacher_schedule_code_classes tc JOIN classes c ON c.id=tc.class_id WHERE tc.code=? ORDER BY c.name").use{p->p.setInt(1,row.code);p.executeQuery().use{rs->buildList{while(rs.next())add(rs.getString(1) to rs.getString(2))}}};row.copy(classIds=cls.map{it.first},classNames=cls.map{it.second})}}}
