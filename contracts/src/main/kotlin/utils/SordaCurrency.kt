package utils

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount

val SordaTokenType = TokenType("Ƨ", 0)

fun SORDA(amount: Int): Amount<TokenType> = com.r3.corda.lib.tokens.contracts.utilities.amount(amount, SordaTokenType)
fun SORDA(amount: Long): Amount<TokenType> = com.r3.corda.lib.tokens.contracts.utilities.amount(amount, SordaTokenType)
fun SORDA(amount: Double): Amount<TokenType> = com.r3.corda.lib.tokens.contracts.utilities.amount(amount, SordaTokenType)
val Int.SORDA: Amount<TokenType> get() = SORDA(this)
val Long.SORDA: Amount<TokenType> get() = SORDA(this)
val Double.SORDA: Amount<TokenType> get() = SORDA(this)
