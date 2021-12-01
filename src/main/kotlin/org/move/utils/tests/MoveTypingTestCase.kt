package org.move.utils.tests

import org.intellij.lang.annotations.Language

abstract class MoveTypingTestCase : MoveTestBase() {
    protected fun doTest(c: Char = '\n') = checkByFile {
        myFixture.type(c)
    }

    protected fun doTestByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
        c: Char = '\n'
    ) =
        checkByText(before.trimIndent(), after.trimIndent()) {
            myFixture.type(c)
        }

}
