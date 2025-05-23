package org.jetbrains.exposed.v1.core

import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.core.transactions.CoreTransactionManager
import org.slf4j.LoggerFactory

/** Base class representing a provider of log messages. */
interface SqlLogger {
    /** Determines how a log message is routed. */
    fun log(context: StatementContext, transaction: Transaction)
}

/** Returns a [org.slf4j.Logger] named specifically for Exposed log messages.  */
val exposedLogger = LoggerFactory.getLogger("Exposed")!!

/** Class representing a provider of log messages sent to standard output stream. */
object StdOutSqlLogger : SqlLogger {
    /** Prints a log message containing the string representation of a complete SQL statement. */
    override fun log(context: StatementContext, transaction: Transaction) {
        println("SQL: ${context.expandArgs(transaction)}")
    }
}

/** Class representing a provider of log messages at DEBUG level. */
object Slf4jSqlDebugLogger : SqlLogger {
    /**
     * Logs a message containing the string representation of a complete SQL statement.
     *
     * **Note:** This is only logged if DEBUG level is currently enabled.
     */
    override fun log(context: StatementContext, transaction: Transaction) {
        @OptIn(InternalApi::class)
        if (exposedLogger.isDebugEnabled) {
            exposedLogger.debug(context.expandArgs(CoreTransactionManager.currentTransaction()))
        }
    }
}

/** Class representing one or more [SqlLogger]s. */
class CompositeSqlLogger : SqlLogger, StatementInterceptor {
    private val loggers: ArrayList<SqlLogger> = ArrayList(2)

    /** Adds an [SqlLogger] instance. */
    fun addLogger(logger: SqlLogger) {
        loggers.add(logger)
    }

    /** Removes an [SqlLogger] instance. */
    fun removeLogger(logger: SqlLogger) {
        loggers.remove(logger)
    }

    override fun log(context: StatementContext, transaction: Transaction) {
        for (logger in loggers) {
            logger.log(context, transaction)
        }
    }

    override fun afterExecution(transaction: Transaction, contexts: List<StatementContext>, executedStatement: PreparedStatementApi) {
        contexts.forEach {
            log(it, transaction)
        }
    }
}
