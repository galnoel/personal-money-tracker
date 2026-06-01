package com.tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tracker.ui.MainActivity
import com.tracker.ui.quickinput.QuickInputActivity

class MoneyTrackerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(56.dp, 56.dp),
            DpSize(120.dp, 120.dp),
            DpSize(250.dp, 120.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(LocalSize.current)
        }
    }

    @Composable
    private fun WidgetContent(size: DpSize) {
        val bgColor = androidx.glance.color.ColorProvider(day = Color(0xCC050505), night = Color(0xCC050505))
        val orbGlowColor = androidx.glance.color.ColorProvider(day = Color(0x5539FF14), night = Color(0x5539FF14))
        val orbRingColor = androidx.glance.color.ColorProvider(day = Color(0x8839FF14), night = Color(0x8839FF14))
        val primaryColor = androidx.glance.color.ColorProvider(day = Color(0xFF39FF14), night = Color(0xFF39FF14))
        val textColor = androidx.glance.color.ColorProvider(day = Color(0xFFECEFF4), night = Color(0xFFECEFF4))
        val subtextColor = androidx.glance.color.ColorProvider(day = Color(0xFF8B95A5), night = Color(0xFF8B95A5))
        val darkBg = androidx.glance.color.ColorProvider(day = Color(0xFF000000), night = Color(0xFF000000))

        when {
            size.width < 100.dp || size.height < 90.dp -> TinyWidget(
                bgColor = bgColor,
                orbGlowColor = orbGlowColor,
                orbRingColor = orbRingColor,
                primaryColor = primaryColor,
                darkBg = darkBg
            )
            size.width < 180.dp || size.height < 120.dp -> CompactWidget(
                bgColor = bgColor,
                primaryColor = primaryColor,
                textColor = textColor,
                darkBg = darkBg
            )
            else -> FullWidget(
                bgColor = bgColor,
                primaryColor = primaryColor,
                textColor = textColor,
                subtextColor = subtextColor,
                darkBg = darkBg
            )
        }
    }

    @Composable
    private fun TinyWidget(
        bgColor: ColorProvider,
        orbGlowColor: ColorProvider,
        orbRingColor: ColorProvider,
        primaryColor: ColorProvider,
        darkBg: ColorProvider
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(bgColor)
                .padding(6.dp)
                .clickable(actionStartActivity<QuickInputActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(18.dp)
                    .background(orbGlowColor)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(orbRingColor)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(14.dp)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                color = darkBg,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactWidget(
        bgColor: ColorProvider,
        primaryColor: ColorProvider,
        textColor: ColorProvider,
        darkBg: ColorProvider
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(bgColor)
                .padding(10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "Money",
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(14.dp)
                    .background(primaryColor)
                    .padding(vertical = 9.dp)
                    .clickable(actionStartActivity<QuickInputActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add",
                    style = TextStyle(
                        color = darkBg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun FullWidget(
        bgColor: ColorProvider,
        primaryColor: ColorProvider,
        textColor: ColorProvider,
        subtextColor: ColorProvider,
        darkBg: ColorProvider
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(bgColor)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                text = "Money Tracker",
                style = TextStyle(
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = "Tap to add transaction",
                style = TextStyle(
                    color = subtextColor,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(14.dp)
                    .background(primaryColor)
                    .padding(vertical = 11.dp)
                    .clickable(actionStartActivity<QuickInputActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+  Quick Add",
                    style = TextStyle(
                        color = darkBg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
