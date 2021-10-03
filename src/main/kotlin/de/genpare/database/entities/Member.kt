package de.genpare.database.entities

import de.genpare.database.tables.MemberTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

class Member(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Member>(MemberTable) {
        fun newMember(init: Member.() -> Unit) =
            new(Random.nextLong(), init)

        fun findByEmail(email: String) =
            transaction { Member.find { MemberTable.email eq email }.firstOrNull() }

        fun findBySessionId(sessionId: Long) =
            transaction { Member.find { MemberTable.sessionId eq sessionId }.firstOrNull() }
    }

    var email by MemberTable.email
    var name by MemberTable.name
    var sessionId by MemberTable.sessionId
}