package com.projectpandora.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/** Decodes a stored "data:image/...;base64,..." avatar into a bitmap for display. */
fun decodeAvatarImage(dataUri: String?): ImageBitmap? = runCatching {
    if(dataUri.isNullOrBlank()) return@runCatching null
    val comma=dataUri.indexOf(',')
    if(comma<0) return@runCatching null
    val bytes=Base64.decode(dataUri.substring(comma+1),Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap()
}.getOrNull()

/** Downscales a picked photo and returns a small "data:image/jpeg;base64,..." string for upload. */
fun imageUriToDataUrl(context: Context,uri: Uri,maxPx: Int=160): String? = runCatching {
    val resolver=context.contentResolver
    val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
    resolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
    if(bounds.outWidth<=0 || bounds.outHeight<=0) return@runCatching null
    var sample=1
    while(bounds.outWidth/sample>maxPx*2 || bounds.outHeight/sample>maxPx*2) sample*=2
    val opts=BitmapFactory.Options().apply {inSampleSize=sample}
    val bitmap=resolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,opts)} ?: return@runCatching null
    val scale=minOf(1f,maxPx.toFloat()/maxOf(bitmap.width,bitmap.height))
    val scaled=if(scale<1f) Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt().coerceAtLeast(1),(bitmap.height*scale).toInt().coerceAtLeast(1),true) else bitmap
    val out=ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG,70,out)
    "data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
}.getOrNull()
