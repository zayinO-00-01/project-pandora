package com.projectpandora.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

fun encodeAvatar(context: Context,uri: Uri): String {
    val resolver=context.contentResolver
    val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
    resolver.openInputStream(uri).use {BitmapFactory.decodeStream(it,null,bounds)}
    require(bounds.outWidth>0 && bounds.outHeight>0) {"请选择有效图片"}
    var sample=1
    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
    val bitmap=resolver.openInputStream(uri).use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})} ?: error("图片读取失败")
    val side=minOf(bitmap.width,bitmap.height)
    val crop=Bitmap.createBitmap(bitmap,(bitmap.width-side)/2,(bitmap.height-side)/2,side,side)
    val scaled=Bitmap.createScaledBitmap(crop,256,256,true)
    try {
        val output=ByteArrayOutputStream()
        for(quality in listOf(80,60,40)) {
            output.reset();scaled.compress(Bitmap.CompressFormat.JPEG,quality,output)
            if(output.size()<=44000)return "data:image/jpeg;base64,"+Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP)
        }
        error("图片过大，请换一张图片")
    } finally {
        if(scaled!==crop)scaled.recycle()
        if(crop!==bitmap)crop.recycle()
        bitmap.recycle()
    }
}
