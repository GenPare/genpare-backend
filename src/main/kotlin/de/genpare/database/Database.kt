package de.genpare.database

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException
import de.genpare.database.tables.MemberTable
import de.genpare.database.tables.SalaryTable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.Logger
import kotlin.properties.Delegates

object Database {
    private const val waitPeriod = 10

    private lateinit var log: Logger
    private lateinit var url: String
    private lateinit var user: String
    private lateinit var password: String
    private var maxRetries by Delegates.notNull<Int>()

    private val db by lazy {
        log.info("Connecting to $url...")

        val db = Database.connect(
            url = url,
            driver = "com.mysql.jdbc.Driver",
            user = user,
            password = password,
        )

        var success = false

        for (i in 0 until maxRetries) {
            // We wanna handle the retries ourself, so we stop Exposed from doing them for us
            transaction(db.transactionManager.defaultIsolationLevel, 0) {
                try {
                    SchemaUtils.create(MemberTable)
                    SchemaUtils.create(SalaryTable)

                    success = true
                } catch (_: CommunicationsException) {
                    log.warn("Failed to connect to the MariaDB instance." +
                            if (i + 1 < maxRetries) " Retrying in ${waitPeriod}s..." else "")
                    runBlocking { delay((waitPeriod * 1000).toLong()) }
                }
            }
        }

        if (!success) throw IllegalStateException("Failed to connect to the MariaDB instance.")

        log.info("Connected to the MariaDB instance.")

        db
    }

    fun init(log: Logger, url: String, user: String, password: String, maxRetries: Int) {
        this.log = log
        this.url = url
        this.user = user
        this.password = password
        this.maxRetries = maxRetries

        db // We need to connect!
    }
}