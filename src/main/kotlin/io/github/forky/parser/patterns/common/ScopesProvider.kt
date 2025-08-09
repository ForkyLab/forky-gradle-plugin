package io.github.forky.parser.patterns.common

import io.github.diskria.dsl.regex.RegexPattern
import io.github.diskria.dsl.regex.combinators.RegexList
import io.github.diskria.dsl.regex.groups.RegexGroup
import io.github.diskria.dsl.regex.primitives.RegexCharacterClass
import io.github.diskria.dsl.regex.ranges.RegexCharacterRange
import io.github.diskria.dsl.regex.ranges.RegexDigitsRange
import io.github.diskria.utils.kotlin.BracketsType
import io.github.diskria.utils.kotlin.Constants
import io.github.forky.parser.scopes.Scope

interface ScopesProvider {

    val scopesPrefix: Char
    val scopesSeparator: String
    val scopesBracketsType: BracketsType?

    fun getScopesRegexPattern(): RegexPattern {
        val scopesListGroup = RegexGroup.ofCaptured(RegexList.of(idPattern, scopesSeparator))
        val bracketedScopes = scopesListGroup.wrapWithBrackets(scopesBracketsType)
        return RegexGroup.of(scopesPrefix.toString() + bracketedScopes).optional()
    }

    fun parseScopes(scopesMatch: MatchGroup?): Set<Scope>? {
        scopesMatch ?: return null

        val scopeIds = scopesMatch.value.split(scopesSeparator)
        val firstScopeCharIndex = scopesMatch.range.first
        return scopeIds
            .runningFold(firstScopeCharIndex) { charIndex, id ->
                charIndex + id.length + scopesSeparator.length
            }
            .dropLast(1)
            .zip(scopeIds) { charIndex, id ->
                Scope(id, charIndex)
            }
            .toSet()
    }

    companion object {
        val idPattern: RegexPattern by lazy {
            val lettersOrDigits = RegexCharacterRange.LATIN + RegexDigitsRange
            val wordPattern = RegexCharacterClass.of(lettersOrDigits).oneOrMore()
            RegexList.of(wordPattern, Constants.Char.HYPHEN)
        }
    }
}
