package com.swordfish.lemuroid.app.mobile.shared.compose.ui

/**
 * The app's help (question and answer, written with a little HTML) as About sections: the bold
 * question is the heading, what follows is the answer in plain words. A pure function, so the
 * About can be tested without a phone.
 */
private val PARAGRAPH_BREAK = Regex("""<br\s*/?>\s*<br\s*/?>""")
private val QUESTION = Regex("""^\s*<b>(.*?)</b>\s*(?:<br\s*/?>)?(.*)$""", RegexOption.DOT_MATCHES_ALL)
private val TAG = Regex("""<[^>]*>""")
private val LINE_BREAK = Regex("""<br\s*/?>""")

private fun plain(html: String): String =
    html.replace(LINE_BREAK, "\n")
        .replace(TAG, "")
        .replace("&#8226;", "")
        .replace("&amp;", "&")
        .lines()
        .joinToString("\n") { it.trim() }
        .trim()

fun helpSections(help: String): List<AboutSection> =
    help.split(PARAGRAPH_BREAK).map { it.trim() }.filter { it.isNotEmpty() }.map { paragraph ->
        val match = QUESTION.find(paragraph)
        if (match != null) {
            AboutSection(plain(match.groupValues[1]), plain(match.groupValues[2]))
        } else {
            AboutSection(null, plain(paragraph))
        }
    }.filter { it.body.isNotEmpty() }
