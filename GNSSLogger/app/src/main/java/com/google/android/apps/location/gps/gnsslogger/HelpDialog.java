/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.location.gps.gnsslogger;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/** The Help Dialog of the Application */
public class HelpDialog extends Dialog {

  private static Context mContext = null;

  public HelpDialog(Context context) {
    super(context);
    mContext = context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setContentView(R.layout.help);
    WebView help = (WebView) findViewById(R.id.helpView);
    help.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("mailto:")) {
              MailTo mt = MailTo.parse(url);
              Intent emailIntent = new Intent(Intent.ACTION_SEND);
              emailIntent.setType("*/*");
              emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GNSSLogger Feedback");
              emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {mt.getTo()});
              emailIntent.putExtra(Intent.EXTRA_TEXT, "");
              mContext.startActivity(Intent.createChooser(emailIntent, "Send Feedback..."));
              return true;
            } else {
              view.loadUrl(url);
            }
            return true;
          }
        });

    String helpText = readRawTextFile(R.raw.help_contents);
    help.loadData(helpText, "text/html; charset=utf-8", "utf-8");
  }

  private String readRawTextFile(int id) {
    InputStream inputStream = mContext.getResources().openRawResource(id);
    InputStreamReader in = new InputStreamReader(inputStream);
    BufferedReader buf = new BufferedReader(in);
    String line;
    StringBuilder text = new StringBuilder();
    try {
      while ((line = buf.readLine()) != null) {
        text.append(line);
      }
    } catch (IOException e) {
      return null;
    }
    return text.toString();
  }
}
