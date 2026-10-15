package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException
import java.util.UUID

@Serializable data class GradeChangeProposalRequest(val gradeId:String?=null,val studentId:String,val subjectId:String,val teacherId:String?=null,val value:Int,val period:String,val academicYear:String,val note:String?=null)
@Serializable data class GradeChangeDecisionRequest(val decision:String,val note:String?=null)
@Serializable data class GradeChangeRequestDto(val id:String,val gradeId:String?,val studentId:String,val studentName:String,val subjectId:String,val subjectName:String,val teacherId:String,val teacherName:String,val currentValue:Int?,val proposedValue:Int,val period:String,val academicYear:String,val note:String?,val requestedBy:String,val requesterName:String,val status:String,val reviewNote:String?,val createdAt:String)
private data class GradeChangeRow(val gradeId:String?,val studentId:String,val subjectId:String,val teacherId:String,val value:Int,val period:String,val year:String,val note:String?,val status:String)

fun Application.configureGradeChangeApi(auth:AuthService){
 routing { route("/api/v1/grade-change-requests") {
  get {
   val user=gradeChangeUser(call,auth)?:return@get
   val where=when(user.role){"MESIMDHENES"->"WHERE r.status='PENDING' AND r.teacher_id=(SELECT id FROM teachers WHERE user_id=?)";"DREJTOR"->"WHERE r.requested_by=?";"ADMINISTRATOR"->"";else->{call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Nuk keni qasje te kërkesat për ndryshim note."));return@get}}
   val rows=Database.connection().use{c->c.prepareStatement("""
    SELECT r.id,r.grade_id,r.student_id,st.full_name,r.subject_id,COALESCE(su.name,r.subject_id),r.teacher_id,COALESCE(t.full_name,r.teacher_id),r.current_value,r.proposed_value,r.period,r.academic_year,r.note,r.requested_by,COALESCE(u.full_name,r.requested_by),r.status,r.review_note,r.created_at
    FROM grade_change_requests r JOIN students st ON st.id=r.student_id LEFT JOIN subjects su ON su.id=r.subject_id LEFT JOIN teachers t ON t.id=r.teacher_id LEFT JOIN users u ON u.id=r.requested_by
    $where ORDER BY CASE WHEN r.status='PENDING' THEN 0 ELSE 1 END,r.created_at DESC
   """.trimIndent()).use{p->if(where.isNotBlank())p.setString(1,user.id);p.executeQuery().use{rs->buildList{while(rs.next())add(GradeChangeRequestDto(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getObject(9)?.let{rs.getInt(9)},rs.getInt(10),rs.getString(11),rs.getString(12),rs.getString(13),rs.getString(14),rs.getString(15),rs.getString(16),rs.getString(17),rs.getTimestamp(18).toString()))}}}}
   call.respond(rows)
  }
  post {
   val user=gradeChangeUser(call,auth)?:return@post
   if(user.role!="DREJTOR"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm Drejtori mund të propozojë ndryshim note."));return@post}
   val req=call.receive<GradeChangeProposalRequest>()
   if(req.studentId.isBlank()||req.subjectId.isBlank()||req.value !in 1..5||req.period.isBlank()||req.academicYear.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Nxënësi, lënda, nota 1–5, periudha dhe viti shkollor janë të detyrueshme."));return@post}
   val id="GCR-"+UUID.randomUUID().toString().take(8).uppercase()
   val created=Database.connection().use{c->c.autoCommit=false;try{
    val existing=req.gradeId?.let{gid->c.prepareStatement("SELECT student_id,subject_id,teacher_id FROM grades WHERE id=?").use{p->p.setString(1,gid);p.executeQuery().use{rs->if(rs.next())Triple(rs.getString(1),rs.getString(2),rs.getString(3))else null}}}
    if(req.gradeId!=null&&existing==null){c.rollback();call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Nota nuk u gjet."));return@use false}
    val studentId=existing?.first?:req.studentId;val subjectId=existing?.second?:req.subjectId;val teacherId=existing?.third?:req.teacherId?.takeIf{it.isNotBlank()}
    if(teacherId==null){c.rollback();call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Zgjidhni mësimdhënësin përgjegjës."));return@use false}
    val assigned=c.prepareStatement("SELECT 1 FROM teacher_subjects ts JOIN students s ON s.class_id=ts.class_id WHERE ts.teacher_id=? AND ts.subject_id=? AND s.id=?").use{p->p.setString(1,teacherId);p.setString(2,subjectId);p.setString(3,studentId);p.executeQuery().use{it.next()}}
    if(!assigned){c.rollback();call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Mësimdhënësi nuk është caktuar për këtë lëndë dhe klasë."));return@use false}
    val duplicate=c.prepareStatement("SELECT 1 FROM grade_change_requests WHERE status='PENDING' AND ((? IS NOT NULL AND grade_id=?) OR (? IS NULL AND grade_id IS NULL AND student_id=? AND subject_id=? AND period=? AND academic_year=?))").use{p->p.setString(1,req.gradeId);p.setString(2,req.gradeId);p.setString(3,req.gradeId);p.setString(4,studentId);p.setString(5,subjectId);p.setString(6,req.period.trim());p.setString(7,req.academicYear.trim());p.executeQuery().use{it.next()}}
    if(duplicate){c.rollback();call.respond(HttpStatusCode.Conflict,ApiError("PENDING_EXISTS","Ekziston kërkesë në pritje për këtë notë."));return@use false}
    val currentValue=req.gradeId?.let{gid->c.prepareStatement("SELECT value FROM grades WHERE id=?").use{q->q.setString(1,gid);q.executeQuery().use{rs->if(rs.next())rs.getInt(1)else null}}}
    c.prepareStatement("INSERT INTO grade_change_requests(id,grade_id,current_value,student_id,subject_id,teacher_id,proposed_value,period,academic_year,note,requested_by,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,\'PENDING\')").use{p->p.setString(1,id);p.setString(2,req.gradeId);if(currentValue==null)p.setNull(3,java.sql.Types.INTEGER)else p.setInt(3,currentValue);p.setString(4,studentId);p.setString(5,subjectId);p.setString(6,teacherId);p.setInt(7,req.value);p.setString(8,req.period.trim());p.setString(9,req.academicYear.trim());p.setString(10,req.note?.trim()?.takeIf{it.isNotBlank()});p.setString(11,user.id);p.executeUpdate()}
    c.commit();true
   }catch(e:SQLException){c.rollback();throw e}finally{c.autoCommit=true}}
   if(created){
    val details=Database.connection().use{c->c.prepareStatement("SELECT r.teacher_id,st.full_name,COALESCE(su.name,r.subject_id),r.proposed_value FROM grade_change_requests r JOIN students st ON st.id=r.student_id LEFT JOIN subjects su ON su.id=r.subject_id WHERE r.id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())listOf(rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4).toString())else null}}}
    if(details!=null)NotificationService.notifyTeacherById(details[0],"Kërkesë për ndryshim note","Drejtori ka propozuar notën ${details[3]} për nxënësin ${details[1]} në lëndën ${details[2]}. Ju lutemi shqyrtojeni kërkesën.","GRADE_CHANGE_REQUEST",id,user.id)
    call.respond(HttpStatusCode.Created,mapOf("id" to id,"status" to "PENDING"))
   }
  }
  post("/{id}/decision") {
   val user=gradeChangeUser(call,auth)?:return@post
   if(user.role!="MESIMDHENES"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm mësimdhënësi përgjegjës mund të vendosë."));return@post}
   val req=call.receive<GradeChangeDecisionRequest>();val decision=req.decision.uppercase();if(decision !in setOf("APPROVE","REJECT")){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Vendimi duhet të jetë APPROVE ose REJECT."));return@post}
   val teacherId=Database.connection().use{c->c.prepareStatement("SELECT id FROM teachers WHERE user_id=? AND active=TRUE").use{p->p.setString(1,user.id);p.executeQuery().use{if(it.next())it.getString(1)else null}}}
   if(teacherId==null){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Profili i mësimdhënësit nuk është i lidhur."));return@post}
   val id=call.parameters["id"].orEmpty()
   val done=Database.connection().use{c->c.autoCommit=false;try{
    val r=c.prepareStatement("SELECT grade_id,student_id,subject_id,teacher_id,proposed_value,period,academic_year,note,status FROM grade_change_requests WHERE id=? FOR UPDATE").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())GradeChangeRow(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9))else null}}
    if(r==null){c.rollback();call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Kërkesa nuk u gjet."));return@use false}
    if(r.teacherId!=teacherId){c.rollback();call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Kërkesa i përket një mësimdhënësi tjetër."));return@use false}
    val stillAssigned=c.prepareStatement("SELECT 1 FROM teacher_subjects ts JOIN students s ON s.class_id=ts.class_id WHERE ts.teacher_id=? AND ts.subject_id=? AND s.id=?").use{p->p.setString(1,r.teacherId);p.setString(2,r.subjectId);p.setString(3,r.studentId);p.executeQuery().use{it.next()}}
    if(!stillAssigned){c.rollback();call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Mësimdhënësi nuk është më i caktuar për këtë lëndë dhe klasë."));return@use false}
    if(r.status!="PENDING"){c.rollback();call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_REVIEWED","Kërkesa është shqyrtuar më parë."));return@use false}
    if(decision=="APPROVE"){
     if(r.gradeId==null){val gid="G-"+UUID.randomUUID().toString().take(8).uppercase();c.prepareStatement("INSERT INTO grades(id,student_id,subject_id,teacher_id,value,period,academic_year,note) VALUES(?,?,?,?,?,?,?,?)").use{p->p.setString(1,gid);p.setString(2,r.studentId);p.setString(3,r.subjectId);p.setString(4,r.teacherId);p.setInt(5,r.value);p.setString(6,r.period);p.setString(7,r.year);p.setString(8,r.note);p.executeUpdate()};c.prepareStatement("UPDATE grade_change_requests SET grade_id=? WHERE id=?").use{p->p.setString(1,gid);p.setString(2,id);p.executeUpdate()}}
     else c.prepareStatement("UPDATE grades SET value=?,period=?,academic_year=?,note=? WHERE id=?").use{p->p.setInt(1,r.value);p.setString(2,r.period);p.setString(3,r.year);p.setString(4,r.note);p.setString(5,r.gradeId);p.executeUpdate()}
    }
    c.prepareStatement("UPDATE grade_change_requests SET status=?,reviewed_by=?,review_note=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?").use{p->p.setString(1,if(decision=="APPROVE")"APPROVED" else "REJECTED");p.setString(2,user.id);p.setString(3,req.note?.trim()?.takeIf{it.isNotBlank()});p.setString(4,id);p.executeUpdate()}
    c.commit();true
   }catch(e:SQLException){c.rollback();throw e}finally{c.autoCommit=true}}
   if(done){
    val requester=Database.connection().use{c->c.prepareStatement("SELECT requested_by FROM grade_change_requests WHERE id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())rs.getString(1)else null}}}
    if(requester!=null)NotificationService.notifyUser(requester,if(decision=="APPROVE")"Ndryshimi i notës u aprovua" else "Ndryshimi i notës u refuzua","Kërkesa ${id} për ndryshimin e notës u ${if(decision=="APPROVE")"aprovua" else "refuzua"} nga mësimdhënësi përgjegjës.","GRADE_CHANGE_DECISION","GRADE_CHANGE_REQUEST",id,user.id)
    if(decision=="APPROVE"){
     val details=Database.connection().use{c->c.prepareStatement("SELECT r.student_id,st.full_name,r.subject_id,COALESCE(su.name,r.subject_id),r.teacher_id,r.grade_id FROM grade_change_requests r JOIN students st ON st.id=r.student_id LEFT JOIN subjects su ON su.id=r.subject_id WHERE r.id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())listOf(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4) ?: id)else null}}}
     if(details!=null)NotificationService.notifyStudentContacts(user.id,details[4],details[0],"Nota u përditësua","Nota për nxënësin ${details[1]} në lëndën ${details[3]} u përditësua pas aprovimit.","GRADE",details[5] ?: id)
    }
    call.respond(mapOf("id" to id,"status" to if(decision=="APPROVE")"APPROVED" else "REJECTED"))
   }
  }
 }}
}

private suspend fun gradeChangeUser(call:io.ktor.server.application.ApplicationCall,auth:AuthService):UserDto?{
 val token=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()}
 val user=token?.let(auth::userFor)
 if(user==null)call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."))
 return user
}
