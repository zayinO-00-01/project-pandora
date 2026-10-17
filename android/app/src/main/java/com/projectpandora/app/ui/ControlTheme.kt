/* Hallmark · native work console · mineral white / deep sea / copper
 * pre-emit critique: P4 H4 E4 S5 R5 V4 */
package com.projectpandora.app.ui

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.R

object ConsoleTokens {
    val Navy=Color(0xFF142F3B)
    val NavyRaised=Color(0xFF213F4B)
    val Paper=Color(0xFFF3EFE5)
    val LandingPaper=Color(0xFFF3EFE5)
    val White=Color(0xFFFAF7EF)
    val Ink=Color(0xFF19313A)
    val Muted=Color(0xFF566A70)
    val Line=Color(0xFFC3D0D0)
    val Copper=Color(0xFFAA4929)
    val CopperLight=Color(0xFFFFB594)
    val Teal=Color(0xFF17655F)
    val TealLight=Color(0xFF89D5C5)
    val Night=Color(0xFF101F27)
    val NightSurface=Color(0xFF1A303A)
    val NightLine=Color(0xFF6B8993)
    val NightMuted=Color(0xFF9BB1B8)
    val Error=Color(0xFFB32635)
    val ErrorLight=Color(0xFFFF8F9B)
    val ErrorPanel=Color(0xFFFFDADB)
    val ErrorNight=Color(0xFF58232A)
}

class FeedbackSettings(val haptic: Boolean, val sound: Boolean, val setHaptic: (Boolean)->Unit, val setSound: (Boolean)->Unit)
val LocalFeedbackSettings=staticCompositionLocalOf {FeedbackSettings(true,true,{},{})}
val LocalControlFeedback=staticCompositionLocalOf<()->Unit> {{}}
enum class Appearance { LIGHT, DARK }
class AppearanceSettings(val mode: Appearance,val select: (Appearance)->Unit)
val LocalAppearance=staticCompositionLocalOf {AppearanceSettings(Appearance.LIGHT,{})}
val ConsoleDisplay=FontFamily(Font(R.font.console_display,FontWeight.SemiBold))

@OptIn(ExperimentalTextApi::class)
@Composable fun ControlTheme(forceLight: Boolean=false,onAppearanceChange: (Boolean)->Unit={},content: @Composable ()->Unit) {
    val context=LocalContext.current
    val view=LocalView.current
    val prefs=remember {context.getSharedPreferences("pandora-feedback",Context.MODE_PRIVATE)}
    var appearance by remember {mutableStateOf(Appearance.entries.firstOrNull {it.name==prefs.getString("appearance","LIGHT")} ?: Appearance.LIGHT)}
    var haptic by remember {mutableStateOf(prefs.getBoolean("haptic",true))}
    var sound by remember {mutableStateOf(prefs.getBoolean("sound",true))}
    val pool=remember {SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()}
    var sample by remember {mutableIntStateOf(0)}
    DisposableEffect(pool) {
        pool.setOnLoadCompleteListener {_,id,status->if(status==0)sample=id}
        pool.load(context,R.raw.control_tap,1)
        onDispose {pool.setOnLoadCompleteListener(null);pool.release()}
    }
    val audio=remember {context.getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    fun playFeedback(withHaptic: Boolean,withSound: Boolean) {
        if(withHaptic)view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        if(withSound && sample!=0 && audio.ringerMode==AudioManager.RINGER_MODE_NORMAL)
            pool.play(sample,0.65f,0.65f,1,0,1f)
    }
    val settings=FeedbackSettings(haptic,sound,{haptic=it;prefs.edit().putBoolean("haptic",it).apply();if(it)playFeedback(true,false)},
        {sound=it;prefs.edit().putBoolean("sound",it).apply();if(it)playFeedback(false,true)})
    val feedback: ()->Unit={playFeedback(haptic,sound)}
    val dark=!forceLight && appearance==Appearance.DARK
    DisposableEffect(dark) {onAppearanceChange(dark);onDispose {}}
    val colors=if(dark)darkColorScheme(
        primary=ConsoleTokens.CopperLight,onPrimary=ConsoleTokens.Navy,
        primaryContainer=ConsoleTokens.Copper,onPrimaryContainer=ConsoleTokens.White,
        secondary=ConsoleTokens.TealLight,onSecondary=ConsoleTokens.Navy,secondaryContainer=ConsoleTokens.NavyRaised,onSecondaryContainer=ConsoleTokens.TealLight,
        background=ConsoleTokens.Night,surface=ConsoleTokens.NightSurface,onSurface=ConsoleTokens.Paper,
        onBackground=ConsoleTokens.Paper,surfaceVariant=ConsoleTokens.NavyRaised,onSurfaceVariant=ConsoleTokens.NightMuted,
        outline=ConsoleTokens.NightLine,outlineVariant=ConsoleTokens.NavyRaised,surfaceTint=ConsoleTokens.TealLight,
        error=ConsoleTokens.ErrorLight,errorContainer=ConsoleTokens.ErrorNight,onErrorContainer=ConsoleTokens.ErrorPanel
    ) else lightColorScheme(
        primary=ConsoleTokens.Copper,onPrimary=ConsoleTokens.White,
        primaryContainer=ConsoleTokens.Navy,onPrimaryContainer=ConsoleTokens.White,
        secondary=ConsoleTokens.Teal,onSecondary=ConsoleTokens.White,secondaryContainer=ConsoleTokens.Line,onSecondaryContainer=ConsoleTokens.Teal,
        background=ConsoleTokens.Paper,surface=ConsoleTokens.White,onSurface=ConsoleTokens.Ink,
        onBackground=ConsoleTokens.Ink,surfaceVariant=ConsoleTokens.Line,onSurfaceVariant=ConsoleTokens.Muted,
        outline=Color.Black,outlineVariant=Color.Black,surfaceTint=ConsoleTokens.Teal,
        error=ConsoleTokens.Error,errorContainer=ConsoleTokens.ErrorPanel,onErrorContainer=ConsoleTokens.Error
    )
    val chinese=FontFamily(listOf(400,500,600,700).map {weight ->
        Font(R.font.noto_sans_sc,FontWeight(weight),variationSettings=FontVariation.Settings(FontVariation.weight(weight)))
    })
    val baseType=Typography(
        headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=28.sp,lineHeight=36.sp),
        headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=32.sp),
        titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=20.sp,lineHeight=28.sp),
        titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=24.sp),
        titleSmall=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=22.sp),
        bodyLarge=TextStyle(fontSize=16.sp,lineHeight=26.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=22.sp),
        bodySmall=TextStyle(fontSize=12.sp,lineHeight=20.sp),labelLarge=TextStyle(fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=20.sp),
        labelMedium=TextStyle(fontSize=12.sp,lineHeight=18.sp),labelSmall=TextStyle(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=16.sp))
    val type=Typography(
        headlineMedium=baseType.headlineMedium.copy(fontFamily=chinese),headlineSmall=baseType.headlineSmall.copy(fontFamily=chinese),
        titleLarge=baseType.titleLarge.copy(fontFamily=chinese),titleMedium=baseType.titleMedium.copy(fontFamily=chinese),titleSmall=baseType.titleSmall.copy(fontFamily=chinese),
        bodyLarge=baseType.bodyLarge.copy(fontFamily=chinese),bodyMedium=baseType.bodyMedium.copy(fontFamily=chinese),bodySmall=baseType.bodySmall.copy(fontFamily=chinese),
        labelLarge=baseType.labelLarge.copy(fontFamily=chinese),labelMedium=baseType.labelMedium.copy(fontFamily=chinese),labelSmall=baseType.labelSmall.copy(fontFamily=chinese))
    CompositionLocalProvider(LocalFeedbackSettings provides settings,LocalControlFeedback provides feedback,
        LocalAppearance provides AppearanceSettings(appearance,{appearance=it;prefs.edit().putString("appearance",it.name).apply()})) {
        MaterialTheme(colorScheme=colors,typography=type,shapes=Shapes(
            extraSmall=RoundedCornerShape(4.dp),small=RoundedCornerShape(4.dp),medium=RoundedCornerShape(10.dp),
            large=RoundedCornerShape(12.dp),extraLarge=RoundedCornerShape(16.dp)),content=content)
    }
}

@Composable fun ControlButton(onClick: ()->Unit, modifier: Modifier=Modifier, enabled: Boolean=true, contentPadding: PaddingValues=ButtonDefaults.ContentPadding,content: @Composable RowScope.()->Unit) {
    val interaction=remember {MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed)0.98f else 1f,animationSpec=tween(90,easing=FastOutSlowInEasing),label="press")
    val feedback=LocalControlFeedback.current
    Button(onClick={feedback();onClick()},enabled=enabled,interactionSource=interaction,
        shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=0.35f)),
        elevation=ButtonDefaults.buttonElevation(defaultElevation=0.dp,pressedElevation=0.dp,disabledElevation=0.dp),
        modifier=modifier.heightIn(min=48.dp).graphicsLayer {scaleX=scale;scaleY=scale;translationY=if(pressed)1.dp.toPx() else 0f},contentPadding=contentPadding,content=content)
}

@Composable fun ScreenHeading(title: String, description: String) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,style=MaterialTheme.typography.headlineSmall,fontFamily=ConsoleDisplay,fontWeight=FontWeight.SemiBold)
        Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun ControlOutlinedButton(onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,
    contentPadding: PaddingValues=ButtonDefaults.ContentPadding,content: @Composable RowScope.()->Unit) {
    val feedback=LocalControlFeedback.current
    OutlinedButton(onClick={feedback();onClick()},modifier=modifier.heightIn(min=48.dp),enabled=enabled,
        shape=MaterialTheme.shapes.small,contentPadding=contentPadding,content=content)
}


@Composable fun ControlTextButton(onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,
    shape: androidx.compose.ui.graphics.Shape=ButtonDefaults.textShape,
    colors: ButtonColors=ButtonDefaults.textButtonColors(),contentPadding: PaddingValues=ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.()->Unit) {
    val feedback=LocalControlFeedback.current
    TextButton(onClick={feedback();onClick()},modifier=modifier,enabled=enabled,shape=shape,colors=colors,contentPadding=contentPadding,content=content)
}


@Composable fun Modifier.panelOutline(shape: androidx.compose.ui.graphics.Shape=MaterialTheme.shapes.medium): Modifier =
    if(LocalAppearance.current.mode==Appearance.LIGHT)border(1.dp,Color.Black,shape) else this
