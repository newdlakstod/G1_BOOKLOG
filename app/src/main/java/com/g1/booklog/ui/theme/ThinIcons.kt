package com.g1.booklog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun thinPath(
    builder: ImageVector.Builder,
    strokeWidth: Float = 1.6f,
    block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
) {
    builder.path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block
    )
}

val ThinHomeIcon: ImageVector by lazy {
    ImageVector.Builder("ThinHome", 24.dp, 24.dp, 24f, 24f).apply {
        // 지붕
        thinPath(this) {
            moveTo(12f, 3f)
            lineTo(21f, 10f)
            verticalLineTo(20f)
            horizontalLineTo(15f)
            verticalLineTo(15f)
            horizontalLineTo(9f)
            verticalLineTo(20f)
            horizontalLineTo(3f)
            verticalLineTo(10f)
            close()
        }
    }.build()
}

val ThinLibraryIcon: ImageVector by lazy {
    ImageVector.Builder("ThinLibrary", 24.dp, 24.dp, 24f, 24f).apply {
        // 왼쪽 페이지
        thinPath(this) {
            moveTo(12f, 5f)
            curveTo(9.5f, 4f, 5f, 4f, 3f, 5f)
            lineTo(3f, 19f)
            curveTo(5f, 18.5f, 9.5f, 18.5f, 12f, 19f)
        }
        // 오른쪽 페이지
        thinPath(this) {
            moveTo(12f, 5f)
            curveTo(14.5f, 4f, 19f, 4f, 21f, 5f)
            lineTo(21f, 19f)
            curveTo(19f, 18.5f, 14.5f, 18.5f, 12f, 19f)
        }
        // 책등 (중앙 세로선)
        thinPath(this) {
            moveTo(12f, 5f); lineTo(12f, 19f)
        }
    }.build()
}

val ThinCalendarIcon: ImageVector by lazy {
    ImageVector.Builder("ThinCalendar", 24.dp, 24.dp, 24f, 24f).apply {
        // 달력 외곽
        thinPath(this) {
            moveTo(5f, 4f)
            horizontalLineTo(19f)
            curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
            verticalLineTo(20f)
            curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
            horizontalLineTo(5f)
            curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
            verticalLineTo(6f)
            curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
            close()
        }
        // 날짜 고리 두 개
        thinPath(this) { moveTo(8f, 2f); verticalLineTo(6f) }
        thinPath(this) { moveTo(16f, 2f); verticalLineTo(6f) }
        // 헤더 구분선
        thinPath(this) { moveTo(3f, 10f); horizontalLineTo(21f) }
    }.build()
}

val ThinNoteIcon: ImageVector by lazy {
    ImageVector.Builder("ThinNote", 24.dp, 24.dp, 24f, 24f).apply {
        // 노트 본체
        thinPath(this) {
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            verticalLineTo(22f)
            horizontalLineTo(4f)
            close()
        }
        // 상단 스프링 바인딩 바
        thinPath(this, strokeWidth = 2.5f) { moveTo(4f, 6f); horizontalLineTo(20f) }
        // 스프링 핀 3개
        thinPath(this) { moveTo(8f, 3f); verticalLineTo(9f) }
        thinPath(this) { moveTo(12f, 3f); verticalLineTo(9f) }
        thinPath(this) { moveTo(16f, 3f); verticalLineTo(9f) }
        // 줄 3개
        thinPath(this) { moveTo(7f, 12f); horizontalLineTo(17f) }
        thinPath(this) { moveTo(7f, 16f); horizontalLineTo(17f) }
        thinPath(this) { moveTo(7f, 20f); horizontalLineTo(13f) }
    }.build()
}

val ThinFriendsIcon: ImageVector by lazy {
    ImageVector.Builder("ThinFriends", 24.dp, 24.dp, 24f, 24f).apply {
        // 뒤쪽 사람 머리 (작은 원, cx=9 cy=6.5 r=2)
        thinPath(this) {
            moveTo(7f, 6.5f)
            arcTo(2f, 2f, 0f, false, true, 11f, 6.5f)
            arcTo(2f, 2f, 0f, false, true, 7f, 6.5f)
            close()
        }
        // 뒤쪽 사람 몸
        thinPath(this) {
            moveTo(5f, 20f)
            curveTo(5f, 16.5f, 6.8f, 14f, 9f, 14f)
            curveTo(10.2f, 14f, 11.3f, 14.7f, 12f, 15.8f)
        }
        // 앞쪽 사람 머리 (큰 원, cx=14.5 cy=7 r=2.5)
        thinPath(this) {
            moveTo(12f, 7f)
            arcTo(2.5f, 2.5f, 0f, false, true, 17f, 7f)
            arcTo(2.5f, 2.5f, 0f, false, true, 12f, 7f)
            close()
        }
        // 앞쪽 사람 몸
        thinPath(this) {
            moveTo(9.5f, 21f)
            curveTo(9.5f, 17.2f, 11.7f, 14.5f, 14.5f, 14.5f)
            curveTo(17.3f, 14.5f, 19.5f, 17.2f, 19.5f, 21f)
        }
    }.build()
}

val ThinStatsIcon: ImageVector by lazy {
    ImageVector.Builder("ThinStats", 24.dp, 24.dp, 24f, 24f).apply {
        // 막대 4개 (높이 다르게)
        thinPath(this, strokeWidth = 2f) { moveTo(5f, 20f); verticalLineTo(13f) }
        thinPath(this, strokeWidth = 2f) { moveTo(10f, 20f); verticalLineTo(7f) }
        thinPath(this, strokeWidth = 2f) { moveTo(15f, 20f); verticalLineTo(4f) }
        thinPath(this, strokeWidth = 2f) { moveTo(20f, 20f); verticalLineTo(10f) }
        // 기준선
        thinPath(this) { moveTo(2f, 20f); horizontalLineTo(22f) }
    }.build()
}
