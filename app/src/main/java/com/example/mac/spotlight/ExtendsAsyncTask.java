package com.example.mac.spotlight;
import android.os.AsyncTask;
import android.os.Looper;

/**
 * Created by Shubhi on 22/04/17.
 */

public class ExtendsAsyncTask extends AsyncTask {

    public MainActivity m;

    public ExtendsAsyncTask() {
        super();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        Looper.prepare();
        m=new MainActivity();
        return null;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
}