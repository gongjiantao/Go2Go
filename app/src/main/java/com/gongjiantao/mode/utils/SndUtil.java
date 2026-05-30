package com.gongjiantao.mode.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

public class SndUtil {
    public static Uri getUri(Context ctx, File file) {
        String authority = ctx.getPackageName().concat(".fileProvider");
        return FileProvider.getUriForFile(ctx, authority, file);
    }

    public static void sendFile(Context ctx, File file, String title) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, getUri(ctx, file));
        share.setType("application/octet-stream");
        ctx.startActivity(Intent.createChooser(share, title));
    }

    public static void sendText(Context ctx, String title, String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        ctx.startActivity(Intent.createChooser(share, title));
    }
}
