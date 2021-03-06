package com.github.florent37.carpacciocontrollers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.util.LruCache;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.florent37.carpaccio.Carpaccio;
import com.github.florent37.carpaccio.CarpaccioHelper;
import com.github.florent37.carpacciocontrollers.adapter.CarpaccioRecyclerViewAdapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by florentchampigny on 21/07/15.
 */
public class CommonViewController {

    private static final String TAG = "CommonViewController";

    public static float dpFromPx(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static int getLayoutIdentifierFromString(Context context, String layoutName){
        if(layoutName.startsWith("R.layout."))
            layoutName = layoutName.replace("R.layout.","");

        final int layoutResId = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
        if(layoutResId == 0) {
            Log.e(TAG, "failed to find view layout " + layoutName);
            return -1;
        }
        return layoutResId;
    }

    public void forInclude(ViewGroup viewGroup, int number) throws  Exception{

        //used viewstup because R.layout... does not provides anything on AndroidStudio EditMode
        if(viewGroup.getChildCount() > 0) {
            View view = viewGroup.getChildAt(0);
            if( view instanceof ViewStub) {
                int layoutResId = ((ViewStub) view).getLayoutResource();
                viewGroup.removeView(view);

                if (layoutResId != -1) {
                    LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
                    for (int i = 0; i < number; ++i) {
                        layoutInflater.inflate(layoutResId, viewGroup, true);
                    }
                }
            }
        }
    }

    public void padding(View view, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom){
        view.setPadding(
                (int)pxFromDp(view.getContext(),paddingLeft),
                (int)pxFromDp(view.getContext(),paddingTop),
                (int)pxFromDp(view.getContext(),paddingRight),
                (int)pxFromDp(view.getContext(),paddingBottom)
        );
    }

    public void margin(View view, int marginLeft, int marginTop, int marginRight, int marginBottom){
        if(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            lp.setMargins(
                    (int)pxFromDp(view.getContext(), marginLeft),
                    (int)pxFromDp(view.getContext(), marginTop),
                    (int)pxFromDp(view.getContext(), marginRight),
                    (int)pxFromDp(view.getContext(), marginBottom)
            );
            view.setLayoutParams(lp);
        }
    }

    public void adapter(View view, String mappedName, String layoutName){
        final int layoutResId = getLayoutIdentifierFromString(view.getContext(),layoutName);
        if(layoutResId != -1) {
            final Carpaccio carpaccio = CarpaccioHelper.findParentCarpaccio(view);
            if (carpaccio != null ) {
                if (view instanceof RecyclerView) {
                    ((RecyclerView) view).setLayoutManager(new LinearLayoutManager(view.getContext()));
                    ((RecyclerView) view).setAdapter(new CarpaccioRecyclerViewAdapter(carpaccio, layoutResId, mappedName));
                } else if (view instanceof AdapterView) {

                }
            }
        }
    }

    public void clickStartActivity(final View view, final String activityName){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Class<?> c = null;
                if (activityName != null) {

                    String acivityPath = activityName;

                    if(acivityPath.startsWith(".")){
                        acivityPath = view.getContext().getPackageName()+acivityPath;
                    }

                    try {
                        c = Class.forName(acivityPath);

                        view.getContext().startActivity(new Intent(view.getContext(), c));
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public <T extends View> T replace(View view, String viewIdentifier) {
        return replaceViewithTagToRemove(view, viewIdentifier, "replace(" + viewIdentifier + ")");
    }

    public <T extends View> T replaceViewithTagToRemove(View view, String viewIdentifier, String tagToRemove) {
        Context context = view.getContext();
        View newView = null;
        if(viewIdentifier.startsWith("R.layout.") || !viewIdentifier.contains("."))
            newView = replaceByLayout(context, view,viewIdentifier);
        else
            newView = replaceByViewClass(context, view,viewIdentifier);

        if(newView != null) {
            ViewGroup parent = (ViewGroup) view.getParent();

            newView.setLayoutParams(view.getLayoutParams());
            newView.setId(view.getId());

            newView.setTag(CarpaccioHelper.removeTag(view,tagToRemove));

            if(view.getBackground() != null)
                newView.setBackgroundDrawable(view.getBackground());

            if(view instanceof ImageView && newView instanceof ImageView){
                ImageView imgView = (ImageView) view;
                ImageView imgNewView = (ImageView) newView;

                if(imgView.getDrawable() != null)
                    imgNewView.setImageDrawable(imgView.getDrawable());
            }

            if(view instanceof TextView && newView instanceof TextView){
                TextView txtView = (TextView) view;
                TextView txtNewView = (TextView) newView;

                if(txtView.getText() != null)
                    txtNewView.setText(txtView.getText());
                if(txtView.getHint() != null)
                    txtNewView.setHint(txtView.getText());
            }

            int index = parent.indexOfChild(view);
            parent.addView(newView,index);
            parent.removeView(view);
        }

        CarpaccioHelper.registerToParentCarpaccio(newView);

        return (T)newView;
    }

    protected View replaceByViewClass(Context context, View view, String viewClassName) { //com.github.florent37.materialviewpager.MaterialViewPager
        View newView = null;
        try{
            Class viewClass = Class.forName(viewClassName);
            Constructor<View> constructor = viewClass.getDeclaredConstructor(Context.class);
            newView = constructor.newInstance(context);
        }catch (Exception e){
            Log.e(TAG,"failed to find view class "+viewClassName,e);
        }

        return newView;
    }

    protected View replaceByLayout(Context context, View view, String layout) { //R.layout.XXX
        if(layout.startsWith("R.layout."))
            layout = layout.replace("R.layout.","");

        int layoutResId = context.getResources().getIdentifier(layout, "layout", context.getPackageName());
        if(layoutResId == 0) {
            Log.e(TAG, "failed to find view layout " + layout);
            return null;
        }
        else {
            ViewGroup parent = (ViewGroup) view.getParent();
            return LayoutInflater.from(view.getContext()).inflate(layoutResId, parent, false);
        }
    }

}
