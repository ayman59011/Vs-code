package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * Robust, high-performance regex-based tokenization syntax highlighting engine
 * for real-time code editor typing.
 */
object SyntaxHighlighter {

    // Default VS Code Modern Theme Palette
    val DEFAULT_KEYWORD_COLOR = Color(0xFF569CD6)   // Blue
    val DEFAULT_STRING_COLOR = Color(0xFFCE9178)    // Orange / Terracotta
    val DEFAULT_COMMENT_COLOR = Color(0xFF6A9955)   // Green
    val DEFAULT_NUMBER_COLOR = Color(0xFFB5CEA8)    // Light Green
    val DEFAULT_TYPE_COLOR = Color(0xFF4EC9B0)      // Teal
    val DEFAULT_FUNCTION_COLOR = Color(0xFFDCDCAA)  // Yellow-Beige
    val DEFAULT_ANNOTATION_COLOR = Color(0xFF9CDCFE) // Sky Blue
    val DEFAULT_OPERATOR_COLOR = Color(0xFFD4D4D4)  // Light Gray
    val DEFAULT_CONSTANT_COLOR = Color(0xFF4FC1FF)  // Cyan
    val DEFAULT_TEXT_COLOR = Color(0xFFD4D4D4)      // Neutral Gray

    // Token classifications
    private enum class TokenType {
        COMMENT,
        STRING,
        ANNOTATION,
        NUMBER,
        KEYWORD,
        TYPE,
        FUNCTION,
        CONSTANT,
        OPERATOR,
        PUNCTUATION
    }

    private data class TokenSpan(
        val start: Int,
        val end: Int,
        val type: TokenType,
        val color: Color? = null,
        val isBold: Boolean = false
    ) : Comparable<TokenSpan> {
        override fun compareTo(other: TokenSpan): Int {
            val startComp = start.compareTo(other.start)
            return if (startComp != 0) startComp else other.end.compareTo(end)
        }
    }

    // Keyword dictionaries
    private val kotlinKeywords = setOf(
        "package", "import", "class", "interface", "object", "enum", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "throw", "try", "catch", "finally", "is", "in", "!in", "!is", "as", "as?",
        "this", "super", "null", "true", "false", "typeof", "constructor", "init",
        "companion", "override", "private", "public", "protected", "internal",
        "open", "final", "abstract", "sealed", "data", "inline", "noinline",
        "crossinline", "reified", "external", "suspend", "tailrec", "operator",
        "infix", "const", "lateinit", "vararg", "by", "get", "set", "where", "yield"
    )

    private val jsKeywords = setOf(
        "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch",
        "class", "const", "continue", "debugger", "default", "delete", "do",
        "double", "else", "enum", "eval", "export", "extends", "false", "final",
        "finally", "float", "for", "function", "goto", "if", "implements", "import",
        "in", "instanceof", "int", "interface", "let", "long", "native", "new",
        "null", "package", "private", "protected", "public", "return", "short",
        "static", "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "true", "try", "typeof", "var", "void", "volatile", "while",
        "with", "yield", "async", "from", "as", "of", "undefined", "NaN"
    )

    private val pythonKeywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break",
        "class", "continue", "def", "del", "elif", "else", "except", "finally",
        "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield", "self"
    )

    private val sqlKeywords = setOf(
        "select", "from", "where", "insert", "into", "update", "delete", "create",
        "drop", "alter", "table", "database", "index", "view", "join", "inner",
        "outer", "left", "right", "full", "cross", "on", "group", "by", "order",
        "having", "limit", "offset", "union", "all", "distinct", "as", "and", "or",
        "not", "in", "is", "null", "like", "between", "exists", "case", "when",
        "then", "else", "end", "primary", "key", "foreign", "references", "values",
        "set", "default", "constraint", "unique", "check", "cascade", "asc", "desc"
    )

    private val htmlKeywords = setOf(
        "html", "head", "title", "base", "link", "meta", "style", "script", "noscript",
        "body", "section", "nav", "article", "aside", "h1", "h2", "h3", "h4", "h5", "h6",
        "header", "footer", "address", "main", "p", "hr", "pre", "blockquote", "ol", "ul",
        "li", "dl", "dt", "dd", "figure", "figcaption", "div", "a", "em", "strong", "small",
        "s", "cite", "q", "dfn", "abbr", "code", "var", "samp", "kbd", "sub", "sup", "i",
        "b", "u", "mark", "span", "br", "wbr", "img", "iframe", "embed", "object", "video",
        "audio", "source", "canvas", "svg", "table", "caption", "colgroup", "col", "tbody",
        "thead", "tfoot", "tr", "td", "th", "form", "fieldset", "legend", "label", "input",
        "button", "select", "datalist", "optgroup", "option", "textarea", "output", "progress"
    )

    private val cssKeywords = setOf(
        "margin", "padding", "display", "position", "top", "bottom", "left", "right",
        "width", "height", "min-width", "min-height", "max-width", "max-height",
        "color", "background", "background-color", "background-image", "border",
        "border-radius", "border-color", "border-width", "box-shadow", "font-family",
        "font-size", "font-weight", "text-align", "text-decoration", "line-height",
        "flex", "flex-direction", "justify-content", "align-items", "align-content",
        "grid", "grid-template-columns", "grid-gap", "gap", "overflow", "z-index",
        "opacity", "transform", "transition", "animation", "cursor", "content", "!important"
    )

    private val commonTypes = setOf(
        "String", "Int", "Boolean", "Double", "Float", "Long", "Short", "Byte", "Char",
        "Any", "Unit", "Nothing", "List", "Map", "Set", "Array", "Pair", "Triple",
        "StateFlow", "MutableStateFlow", "Flow", "ViewModel", "Composable", "Modifier",
        "Number", "Object", "Promise", "ArrayBuffer", "Date", "RegExp", "Error", "JSON"
    )

    /**
     * Primary entry point for highlighting code dynamically with custom colors.
     */
    fun highlight(
        code: String,
        language: String,
        customKeywordColor: Color = DEFAULT_KEYWORD_COLOR,
        customStringColor: Color = DEFAULT_STRING_COLOR,
        customCommentColor: Color = DEFAULT_COMMENT_COLOR,
        customNumberColor: Color = DEFAULT_NUMBER_COLOR,
        customTypeColor: Color = DEFAULT_TYPE_COLOR,
        customFunctionColor: Color = DEFAULT_FUNCTION_COLOR
    ): AnnotatedString {
        if (code.isEmpty()) {
            return buildAnnotatedString { }
        }

        val spans = tokenize(
            code = code,
            language = language.lowercase().trim(),
            keywordColor = customKeywordColor,
            stringColor = customStringColor,
            commentColor = customCommentColor,
            numberColor = customNumberColor,
            typeColor = customTypeColor,
            functionColor = customFunctionColor
        )

        return buildAnnotatedString {
            append(code)
            // Base style across entire text
            addStyle(
                SpanStyle(color = DEFAULT_TEXT_COLOR, fontFamily = FontFamily.Monospace),
                0,
                code.length
            )

            // Apply all non-overlapping spans
            for (span in spans) {
                val clampedStart = span.start.coerceIn(0, code.length)
                val clampedEnd = span.end.coerceIn(0, code.length)
                if (clampedStart < clampedEnd) {
                    val color = span.color ?: when (span.type) {
                        TokenType.KEYWORD -> customKeywordColor
                        TokenType.STRING -> customStringColor
                        TokenType.COMMENT -> customCommentColor
                        TokenType.NUMBER -> customNumberColor
                        TokenType.TYPE -> customTypeColor
                        TokenType.FUNCTION -> customFunctionColor
                        TokenType.ANNOTATION -> DEFAULT_ANNOTATION_COLOR
                        TokenType.CONSTANT -> DEFAULT_CONSTANT_COLOR
                        TokenType.OPERATOR -> DEFAULT_OPERATOR_COLOR
                        TokenType.PUNCTUATION -> DEFAULT_TEXT_COLOR
                    }
                    addStyle(
                        SpanStyle(
                            color = color,
                            fontWeight = if (span.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        ),
                        clampedStart,
                        clampedEnd
                    )
                }
            }
        }
    }

    /**
     * High-speed tokenization pass using prioritized regex intervals.
     */
    private fun tokenize(
        code: String,
        language: String,
        keywordColor: Color,
        stringColor: Color,
        commentColor: Color,
        numberColor: Color,
        typeColor: Color,
        functionColor: Color
    ): List<TokenSpan> {
        val occupiedRanges = BooleanArray(code.length)
        val tokenList = mutableListOf<TokenSpan>()

        // 1. Pass 1: Multi-line and Single-line Comments (Highest precedence)
        val commentPatterns = when (language) {
            "sql" -> listOf(
                Pattern.compile("--.*"),
                Pattern.compile("/\\*[\\s\\S]*?\\*/")
            )
            "python", "py", "sh", "bash" -> listOf(
                Pattern.compile("#.*"),
                Pattern.compile("\"\"\"[\\s\\S]*?\"\"\""),
                Pattern.compile("'''[\\s\\S]*?'''")
            )
            "html", "xml", "svg" -> listOf(
                Pattern.compile("<!--[\\s\\S]*?-->")
            )
            else -> listOf(
                Pattern.compile("//.*"),
                Pattern.compile("/\\*[\\s\\S]*?\\*/")
            )
        }

        for (pattern in commentPatterns) {
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (markRange(occupiedRanges, start, end)) {
                    tokenList.add(TokenSpan(start, end, TokenType.COMMENT, commentColor))
                }
            }
        }

        // 2. Pass 2: String Literals (Double quotes, single quotes, template strings)
        val stringPatterns = listOf(
            Pattern.compile("\"\"\"[\\s\\S]*?\"\"\""),
            Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\""),
            Pattern.compile("'(?:\\\\.|[^'\\\\])*'"),
            Pattern.compile("`(?:\\\\.|[^`\\\\])*`")
        )

        for (pattern in stringPatterns) {
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (markRange(occupiedRanges, start, end)) {
                    tokenList.add(TokenSpan(start, end, TokenType.STRING, stringColor))
                }
            }
        }

        // 3. Pass 3: Annotations / Decorators (@Composable, @Override, @decorator)
        val annotationPattern = Pattern.compile("@[a-zA-Z_]\\w*")
        val annotMatcher = annotationPattern.matcher(code)
        while (annotMatcher.find()) {
            val start = annotMatcher.start()
            val end = annotMatcher.end()
            if (markRange(occupiedRanges, start, end)) {
                tokenList.add(TokenSpan(start, end, TokenType.ANNOTATION, DEFAULT_ANNOTATION_COLOR, isBold = true))
            }
        }

        // 4. Pass 4: Numeric Literals (Hex, Binary, Floats, Decimals)
        val numberPattern = Pattern.compile("\\b(?:0x[0-9a-fA-F]+|0b[01]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFLlDd]?)\\b")
        val numMatcher = numberPattern.matcher(code)
        while (numMatcher.find()) {
            val start = numMatcher.start()
            val end = numMatcher.end()
            if (markRange(occupiedRanges, start, end)) {
                tokenList.add(TokenSpan(start, end, TokenType.NUMBER, numberColor))
            }
        }

        // 5. Pass 5: Keywords
        val keywords = when (language) {
            "kotlin", "kt", "kts", "java" -> kotlinKeywords
            "javascript", "js", "typescript", "ts", "jsx", "tsx" -> jsKeywords
            "python", "py" -> pythonKeywords
            "sql" -> sqlKeywords
            "html", "xml", "svg" -> htmlKeywords
            "css", "scss" -> cssKeywords
            else -> kotlinKeywords + jsKeywords
        }

        for (word in keywords) {
            val patternStr = if (language == "sql") "\\b(?i)$word\\b" else "\\b$word\\b"
            val pattern = Pattern.compile(patternStr)
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (markRange(occupiedRanges, start, end)) {
                    tokenList.add(TokenSpan(start, end, TokenType.KEYWORD, keywordColor, isBold = true))
                }
            }
        }

        // 6. Pass 6: Common and PascalCase Types / Classes
        for (type in commonTypes) {
            val pattern = Pattern.compile("\\b$type\\b")
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (markRange(occupiedRanges, start, end)) {
                    tokenList.add(TokenSpan(start, end, TokenType.TYPE, typeColor))
                }
            }
        }

        // PascalCase Class/Interface identifiers
        val pascalCasePattern = Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b")
        val pascalMatcher = pascalCasePattern.matcher(code)
        while (pascalMatcher.find()) {
            val start = pascalMatcher.start()
            val end = pascalMatcher.end()
            if (markRange(occupiedRanges, start, end)) {
                tokenList.add(TokenSpan(start, end, TokenType.TYPE, typeColor))
            }
        }

        // 7. Pass 7: Function Calls / Declarations (e.g. `doSomething(...)`)
        val functionPattern = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*(?=\\()")
        val funcMatcher = functionPattern.matcher(code)
        while (funcMatcher.find()) {
            val start = funcMatcher.start(1)
            val end = funcMatcher.end(1)
            if (markRange(occupiedRanges, start, end)) {
                tokenList.add(TokenSpan(start, end, TokenType.FUNCTION, functionColor))
            }
        }

        // 8. Pass 8: Operators and Symbols
        val operatorPattern = Pattern.compile("==|!=|<=|>=|=>|->|\\+=|-=|\\*=|/=|&&|\\|\\||[+\\-*/%<>=!&|^~?:]")
        val opMatcher = operatorPattern.matcher(code)
        while (opMatcher.find()) {
            val start = opMatcher.start()
            val end = opMatcher.end()
            if (markRange(occupiedRanges, start, end)) {
                tokenList.add(TokenSpan(start, end, TokenType.OPERATOR, DEFAULT_OPERATOR_COLOR))
            }
        }

        return tokenList
    }

    /**
     * Checks if the given range is completely free in [occupiedRanges],
     * and marks it as occupied if so. Returns true if marked successfully.
     */
    private fun markRange(occupied: BooleanArray, start: Int, end: Int): Boolean {
        if (start < 0 || end > occupied.size || start >= end) return false
        for (i in start until end) {
            if (occupied[i]) return false
        }
        for (i in start until end) {
            occupied[i] = true
        }
        return true
    }
}
