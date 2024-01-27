package org.flexatar;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class FlxLayoutHelper {
    public static TextView textViewFactory(Context context,String text){
        TextView textView1 = new TextView(context);
        textView1.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView1.setText(text);
        textView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        textView1.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        return textView1;
    }

    public static LinearLayout horizontalLayoutFactory(Context context, View[] views){
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : views) {
            layout.addView(v,p);
        }

        return layout;
    }
}

