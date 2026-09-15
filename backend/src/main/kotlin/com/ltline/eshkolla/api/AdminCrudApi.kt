package com.ltline.eshkolla.api

import com.ltline.eshkolla.auth.AuthService
import com.ltline.eshkolla.db.Database
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.sql.SQLException

@Serializable data class AdminSchoolUpdate(val name:String,val address:String?=null)
@Serializable data class AdminSubjectUpdate(val name:String,val code:String?=null)
@Serializable data class AdminTeacherUpdate(val fullName:String,val username:String,val subjectId:String)
@Serializable data class AdminStudentUpdate(val fullName:String,val classId:String,val birthDate:String)
@Serializable data class AdminUserUpdate(val username:String,val fullName:String,val role:String,val active:Boolean)

fun Application.configureAdminCrudApi(authService:AuthService){
 routing{route("/api/v1/management"){
  put("/schools/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminSchoolUpdate>();if(r.name.trim().length<2){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri i shkollës është i detyrueshëm."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE schools SET name=?,address=? WHERE id=?").use{p->p.setString(1,r.name.trim());p.setString(2,r.address?.trim()?.takeIf{it.isNotBlank()});p.setString(3,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Shkolla nuk u gjet."))else call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Shkolla nuk mund të ndryshohet me këto të dhëna."))}}
  delete("/schools/{id}"){if(!adminOnly(call,authService))return@delete;softDelete(call,authService,call.parameters["id"].orEmpty(),"schools","Shkolla")}

  put("/subjects/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminSubjectUpdate>();if(r.name.trim().length<2){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri i lëndës është i detyrueshëm."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE subjects SET name=?,code=? WHERE id=?").use{p->p.setString(1,r.name.trim());p.setString(2,r.code?.trim()?.takeIf{it.isNotBlank()});p.setString(3,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Lënda nuk u gjet."))else call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Lënda ose kodi ekziston."))}}
  delete("/subjects/{id}"){if(!adminOnly(call,authService))return@delete;softDelete(call,authService,call.parameters["id"].orEmpty(),"subjects","Lënda")}

  put("/teachers/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminTeacherUpdate>();if(r.fullName.trim().length<2||r.username.trim().length<3||r.subjectId.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri, përdoruesi dhe lënda janë të detyrueshme."));return@put};if(!existsActive("subjects",r.subjectId)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Lënda nuk ekziston."));return@put};try{val n=Database.connection().use{c->c.prepareStatement("UPDATE teachers SET full_name=?,subject_id=? WHERE id=?").use{p->p.setString(1,r.fullName.trim());p.setString(2,r.subjectId.trim());p.setString(3,id);p.executeUpdate()}};if(n==0){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mësimdhënësi nuk u gjet."));return@put};Database.connection().use{c->c.prepareStatement("UPDATE users SET username=?,full_name=? WHERE id=(SELECT user_id FROM teachers WHERE id=?)").use{p->p.setString(1,r.username.trim());p.setString(2,r.fullName.trim());p.setString(3,id);p.executeUpdate()}};call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Përdoruesi ekziston ose të dhënat nuk janë valide."))}}
  delete("/teachers/{id}"){if(!adminOnly(call,authService))return@delete;val id=call.parameters["id"].orEmpty();val n=Database.connection().use{c->c.prepareStatement("UPDATE teachers SET active=FALSE WHERE id=?").use{p->p.setString(1,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Mësimdhënësi nuk u gjet."))else call.respond(HttpStatusCode.NoContent)}

  put("/students/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminStudentUpdate>();if(r.fullName.trim().length<2||r.classId.isBlank()||r.birthDate.isBlank()){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Emri, klasa dhe datëlindja janë të detyrueshme."));return@put};if(!existsActive("classes",r.classId)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Klasa nuk ekziston."));return@put};val n=Database.connection().use{c->c.prepareStatement("UPDATE students SET full_name=?,class_id=?,birth_date=? WHERE id=?").use{p->p.setString(1,r.fullName.trim());p.setString(2,r.classId);p.setString(3,r.birthDate.trim());p.setString(4,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Nxënësi nuk u gjet."))else call.respond(mapOf("id" to id))}
  delete("/students/{id}"){if(!adminOnly(call,authService))return@delete;softDelete(call,authService,call.parameters["id"].orEmpty(),"students","Nxënësi")}

  put("/users/{id}"){if(!adminOnly(call,authService))return@put;val id=call.parameters["id"].orEmpty();val r=call.receive<AdminUserUpdate>();val allowed=setOf("ADMINISTRATOR","DREJTOR","MESIMDHENES","NXENES","PRIND");if(r.username.trim().length<3||r.fullName.trim().length<2||r.role !in allowed){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Përdoruesi, emri dhe roli janë të detyrueshme."));return@put};val current=Database.connection().use{c->c.prepareStatement("SELECT role FROM users WHERE id=?").use{p->p.setString(1,id);p.executeQuery().use{rs->if(rs.next())rs.getString(1)else null}}};if(current==null){call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","Përdoruesi nuk u gjet."));return@put};val me=call.currentAdmin(authService);if(id==me&&!r.active){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund të çaktivizojë llogarinë e vet."));return@put};try{Database.connection().use{c->c.prepareStatement("UPDATE users SET username=?,full_name=?,role=?,active=? WHERE id=?").use{p->p.setString(1,r.username.trim());p.setString(2,r.fullName.trim());p.setString(3,r.role);p.setBoolean(4,r.active);p.setString(5,id);p.executeUpdate()}};call.respond(mapOf("id" to id))}catch(_:SQLException){call.respond(HttpStatusCode.Conflict,ApiError("ALREADY_EXISTS","Emri i përdoruesit ekziston."))}}
  delete("/users/{id}"){if(!adminOnly(call,authService))return@delete;val id=call.parameters["id"].orEmpty();if(id==call.currentAdmin(authService)){call.respond(HttpStatusCode.BadRequest,ApiError("VALIDATION_ERROR","Administratori nuk mund të fshijë llogarinë e vet."));return@delete};softDelete(call,authService,id,"users","Përdoruesi")}
 }}
}

private suspend fun adminOnly(call:ApplicationCall,auth:AuthService):Boolean{val t=call.request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()};val u=t?.let(auth::userFor);if(u==null){call.respond(HttpStatusCode.Unauthorized,ApiError("UNAUTHORIZED","Kyçja është e nevojshme."));return false};if(u.role!="ADMINISTRATOR"){call.respond(HttpStatusCode.Forbidden,ApiError("FORBIDDEN","Vetëm administratori ka këtë qasje."));return false};return true}
private suspend fun ApplicationCall.currentAdmin(auth:AuthService):String?{val t=request.headers["Authorization"]?.removePrefix("Bearer ")?.takeIf{it.isNotBlank()};return t?.let(auth::userFor)?.id}
private fun existsActive(table:String,id:String):Boolean{val safe=setOf("subjects","classes").firstOrNull{it==table}?:return false;return Database.connection().use{c->c.prepareStatement("SELECT 1 FROM $safe WHERE id=? AND active=TRUE").use{p->p.setString(1,id);p.executeQuery().use{it.next()}}}}
private suspend fun softDelete(call:ApplicationCall,auth:AuthService,id:String,table:String,label:String){val safe=setOf("schools","subjects","students","users").firstOrNull{it==table};if(safe==null){call.respond(HttpStatusCode.InternalServerError,ApiError("SERVER_ERROR","Operacioni nuk lejohet."));return};val n=Database.connection().use{c->c.prepareStatement("UPDATE $safe SET active=FALSE WHERE id=?").use{p->p.setString(1,id);p.executeUpdate()}};if(n==0)call.respond(HttpStatusCode.NotFound,ApiError("NOT_FOUND","$label nuk u gjet."))else call.respond(HttpStatusCode.NoContent)}
