package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException
import java.util.UUID

@Serializable data class AdminSchoolUpdate(val name:String,val address:String?=null)
@Serializable data class AdminSubjectUpdate(val name:String,val code:String?=null)
@Serializable data class AdminTeacherUpdate(val fullName:String,val username:String,val subjectId:String)
@Serializable data class AdminUserUpdate(val username:String,val fullName:String,val role:String,val active:Boolean,val studentId:String?=null)
@Serializable data class AdminAbsenceUpdate(val studentId:String,val subjectId:String,val teacherId:String,val date:String,val status:String,val note:String?=null)
@Serializable data class AdminAbsenceDto(val id:String,val studentId:String,val subjectId:String,val teacherId:String,val date:String,val status:String,val note:String?=null)

fun Application.configureAdminCrudApi(authService:AuthService){
 routing{route("/api/v1/management"){
  put("/schools/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminSchoolUpdate>();if(r.name.trim().length<2){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri i shkollës është i detyrueshëm."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE schools SET name=?,address=? WHERE id=?").use{p->p.setString(1,r.name.trim());p.setString(2,r.address?.trim()?.takeIf{it.isNotBlank()});p.setString(3,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Shkolla nuk u gjet."))else call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Shkolla nuk mund të ndryshohet me këto të dhëna."))}}
  delete("/schools/{id}"){if(!adminOnly(call,authService))return@delete;softDelete(call,authService,call.parameters["id"].orEmpty(),"schools","Shkolla")}

  put("/subjects/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminSubjectUpdate>();if(r.name.trim().length<2){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri i lëndës është i detyrueshëm."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE subjects SET name=?,code=? WHERE id=?").use{p->p.setString(1,r.name.trim());p.setString(2,r.code?.trim()?.takeIf{it.isNotBlank()});p.setString(3,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Lënda nuk u gjet."))else call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Lënda ose kodi ekziston."))}}
  delete("/subjects/{id}"){if(!adminOnly(call,authService))return@delete;softDelete(call,authService,call.parameters["id"].orEmpty(),"subjects","Lënda")}

  put("/teachers/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminTeacherUpdate>();if(r.fullName.trim().length<2||r.username.trim().length<3||r.subjectId.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri, përdoruesi dhe lënda janë të detyrueshme."));return@put};if(!existsActive("subjects",r.subjectId)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Lënda nuk ekziston."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE teachers SET full_name=?,subject_id=? WHERE id=?").use{p->p.setString(1,r.fullName.trim());p.setString(2,r.subjectId.trim());p.setString(3,id);p.executeUpdate()}};if(n==0){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mësimdhënësi nuk u gjet."));return@put};Database.connection().use{c->c.prepareStatement("UPDATE users SET username=?,full_name=? WHERE id=(SELECT user_id FROM teachers WHERE id=?)").use{p->p.setString(1,r.username.trim());p.setString(2,r.fullName.trim());p.setString(3,id);p.executeUpdate()}};call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Përdoruesi ekziston ose të dhënat nuk janë valide."))}}
  delete("/teachers/{id}"){if(!adminOnly(call,authService))return@delete;val id=call.parameters["id"].orEmpty();val n=Database.connection().use{c->c.prepareStatement("UPDATE teachers SET active=FALSE WHERE id=?").use{p->p.setString(1,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mësimdhënësi nuk u gjet."))else call.respond(HttpStatusCode.NoContent)}
  put("/users/{id}"){
   if(!adminOnly(call,authService))return@put
   val id=call.parameters["id"].orEmpty()
   val r=call.receive<AdminUserUpdate>()
   val role=r.role.trim()
   val studentId=r.studentId?.trim()?.takeIf{it.isNotBlank()}
   val allowed=setOf("ADMINISTRATOR","DREJTOR","MESIMDHENES","NXENES","PRIND")
   if(r.username.trim().length<3||r.fullName.trim().length<2||role !in allowed){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Përdoruesi, emri dhe roli janë të detyrueshme."));return@put}
   if(role in setOf("PRIND","NXENES")&&studentId.isNullOrBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Për rolin Prind ose Nxënës duhet të zgjidhet nxënësi i lidhur."));return@put}
   if(role in setOf("PRIND","NXENES")&&!existsActive("students",studentId!!)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Nxënësi i zgjedhur nuk ekziston ose nuk është aktiv."));return@put}
   val current=Database.connection().use{c->c.prepareStatement("SELECT role FROM users WHERE id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())rs.getString(1)else null}}}
   if(current==null){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Përdoruesi nuk u gjet."));return@put}
   val me=call.currentAdmin(authService)
   if(id==me&&!r.active){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund të çaktivizojë llogarinë e vet."));return@put}
   if(id==me&&role!="ADMINISTRATOR"){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund ta ndryshojë rolin e llogarisë së vet."));return@put}
   try{
    Database.connection().use{c->
     c.autoCommit=false
     try{
      c.prepareStatement("UPDATE users SET username=?,full_name=?,role=?,active=? WHERE id=?").use{p->p.setString(1,r.username.trim());p.setString(2,r.fullName.trim());p.setString(3,role);p.setBoolean(4,r.active);p.setString(5,id);p.executeUpdate()}
      c.prepareStatement("DELETE FROM parent_students WHERE user_id=?").use{p->p.setString(1,id);p.executeUpdate()}
      c.prepareStatement("DELETE FROM student_users WHERE user_id=?").use{p->p.setString(1,id);p.executeUpdate()}
      when(role){
       "PRIND"->c.prepareStatement("INSERT INTO parent_students(user_id,student_id) VALUES (?,?)").use{p->p.setString(1,id);p.setString(2,studentId);p.executeUpdate()}
       "NXENES"->c.prepareStatement("INSERT INTO student_users(user_id,student_id) VALUES (?,?)").use{p->p.setString(1,id);p.setString(2,studentId);p.executeUpdate()}
      }
      if(role=="MESIMDHENES"){
       val changed=c.prepareStatement("UPDATE teachers SET full_name=?,active=TRUE WHERE user_id=?").use{p->p.setString(1,r.fullName.trim());p.setString(2,id);p.executeUpdate()}
       if(changed==0)c.prepareStatement("INSERT INTO teachers(id,user_id,full_name,subject_id,active) VALUES (?,?,?,NULL,TRUE)").use{p->p.setString(1,"M-"+UUID.randomUUID().toString().take(8).uppercase());p.setString(2,id);p.setString(3,r.fullName.trim());p.executeUpdate()}
      }else c.prepareStatement("UPDATE teachers SET active=FALSE WHERE user_id=?").use{p->p.setString(1,id);p.executeUpdate()}
      c.commit()
     }catch(e:SQLException){c.rollback();throw e}finally{c.autoCommit=true}
    }
    if(current!=role||!r.active)authService.invalidateUserSessions(id)
    call.respond(mapOf("id" to id))
   }catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Emri i përdoruesit ekziston ose lidhjet nuk janë valide."))}
  }
  delete("/users/{id}"){if(!adminOnly(call,authService))return@delete;val id=call.parameters["id"].orEmpty();if(id==call.currentAdmin(authService)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund të fshijë llogarinë e vet."));return@delete};softDelete(call,authService,id,"users","Përdoruesi")}
   post("/absences"){
    if(!adminOnly(call,authService))return@post
    val actor=call.currentAdmin(authService) ?: return@post
    val r=call.receive<AdminAbsenceUpdate>()
    validateAdminAbsence(r)?.let{call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR",it));return@post}
    val id="ABS-"+UUID.randomUUID().toString().take(8).uppercase()
    Database.connection().use{db->db.prepareStatement("INSERT INTO absences(id,student_id,subject_id,teacher_id,date,status,note) VALUES(?,?,?,?,?,?,?)").use{p->p.setString(1,id);p.setString(2,r.studentId);p.setString(3,r.subjectId);p.setString(4,r.teacherId);p.setString(5,r.date);p.setString(6,r.status);p.setString(7,r.note?.takeIf{it.isNotBlank()});p.executeUpdate()}}
    NotificationService.notifyStudentContacts(actor,r.teacherId,r.studentId,"U regjistrua mungesa","U regjistrua mungesa për nxënësin ${adminStudentName(r.studentId)} më ${r.date}. Statusi: ${r.status}.","ABSENCE",id)
    call.respond(HttpStatusCode.Created,AdminAbsenceDto(id,r.studentId,r.subjectId,r.teacherId,r.date,r.status,r.note))
   }
   put("/absences/{id}"){
    if(!adminOnly(call,authService))return@put
    val actor=call.currentAdmin(authService) ?: return@put
    val id=call.parameters["id"].orEmpty()
    val r=call.receive<AdminAbsenceUpdate>()
    validateAdminAbsence(r)?.let{call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR",it));return@put}
    val n=Database.connection().use{db->db.prepareStatement("UPDATE absences SET student_id=?,subject_id=?,teacher_id=?,date=?,status=?,note=? WHERE id=?").use{p->p.setString(1,r.studentId);p.setString(2,r.subjectId);p.setString(3,r.teacherId);p.setString(4,r.date);p.setString(5,r.status);p.setString(6,r.note?.takeIf{it.isNotBlank()});p.setString(7,id);p.executeUpdate()}}
    if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mungesa nuk u gjet."))else{
     NotificationService.notifyStudentContacts(actor,r.teacherId,r.studentId,"Mungesa u ndryshua","Evidenca e mungesës për nxënësin ${adminStudentName(r.studentId)} më ${r.date} u ndryshua. Statusi: ${r.status}.","ABSENCE",id)
     call.respond(AdminAbsenceDto(id,r.studentId,r.subjectId,r.teacherId,r.date,r.status,r.note))
    }
   }
   delete("/absences/{id}"){
    if(!adminOnly(call,authService))return@delete
    val actor=call.currentAdmin(authService) ?: return@delete
    val id=call.parameters["id"].orEmpty()
    val existing=Database.connection().use{db->db.prepareStatement("SELECT student_id,subject_id,teacher_id,date FROM absences WHERE id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())listOf(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4))else null}}}
    if(existing==null){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mungesa nuk u gjet."));return@delete}
    val n=Database.connection().use{db->db.prepareStatement("DELETE FROM absences WHERE id=?").use{p->p.setString(1,id);p.executeUpdate()}}
    if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mungesa nuk u gjet."))else{
     NotificationService.notifyStudentContacts(actor,existing[2],existing[0],"Mungesa u fshi","Evidenca e mungesës për nxënësin ${adminStudentName(existing[0])} më ${existing[3]} u fshi.","ABSENCE",id)
     call.respond(HttpStatusCode.NoContent)
    }
   }
 }}
}

private suspend fun adminOnly(call:ApplicationCall,auth:AuthService):Boolean{val t=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()};val u=t?.let(auth::userFor);if(u==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return false};if(u.role!="ADMINISTRATOR"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori ka këtë qasje."));return false};return true}
private suspend fun ApplicationCall.currentAdmin(auth:AuthService):String?{val t=request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()};return t?.let(auth::userFor)?.id}
private fun existsActive(table:String,id:String):Boolean{val safe=setOf("subjects","classes","students").firstOrNull{it==table}?:return false;return Database.connection().use{c->c.prepareStatement("SELECT 1 FROM $safe WHERE id=? AND active=TRUE").use{p->p.setString(1,id);p.executeQuery().use{it.next()}}}}
private suspend fun softDelete(call:ApplicationCall,auth:AuthService,id:String,table:String,label:String){val safe=setOf("schools","subjects","students","users").firstOrNull{it==table};if(safe==null){call.respond(HttpStatusCode.InternalServerError,ApiError("SERVER_ERROR","Operacioni nuk lejohet."));return};val n=Database.connection().use{c->c.prepareStatement("UPDATE $safe SET active=FALSE WHERE id=?").use{p->p.setString(1,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","$label nuk u gjet."))else call.respond(HttpStatusCode.NoContent)}
private fun validateAdminAbsence(r:AdminAbsenceUpdate):String?{if(r.studentId.isBlank())return "Nxënësi është i detyrueshëm.";if(r.subjectId.isBlank())return "Lënda është e detyrueshme.";if(r.teacherId.isBlank())return "Mësimdhënësi është i detyrueshëm.";if(r.date.isBlank())return "Data është e detyrueshme.";if(r.status !in setOf("E_ARSYESHME","E_PAAFTESUAR"))return "Statusi i mungesës nuk është valid.";return null}

private fun adminStudentName(studentId:String):String=Database.connection().use{db->db.prepareStatement("SELECT full_name FROM students WHERE id=?").use{p->p.setString(1,studentId);p.executeQuery().use{rs->if(rs.next())rs.getString(1)else studentId}}}
