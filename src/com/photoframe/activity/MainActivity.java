package com.photoframe.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.*;
import com.photoframe.*;
import com.photoframe.adapters.MyAdapterListItem;
import com.photoframe.auth.StartActivity;
import com.photoframe.util.MyConst;
import com.photoframe.util.MyLoader;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<List<ListItem>> {
    private String directoryPath;
    private Credentials credentials;
    private MyAdapterListItem<ListItem> arrayAdapter;
    private ListView listView;
    public ArrayList<ListItem> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(StartActivity.TOKEN, "Nothing");
        String username = preferences.getString(StartActivity.USERNAME, "Nothing");
        credentials = new Credentials(username, token);

        directoryPath = getIntent().getStringExtra(MyConst.DIRECTORY_PATH);
        TextView way = (TextView) findViewById(R.id.mainActivityWayToDirictory);
        way.setTextSize(30);
        way.setText(directoryPath);

        arrayAdapter = new MyAdapterListItem(getApplicationContext(), R.layout.list_item);
        listView = (ListView) findViewById(R.id.mainActivityListView);
        list = new ArrayList<ListItem>();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!list.get(i).isCollection()) {
                    if (list.get(i).getContentType().indexOf("image") != 0) {
                        Toast.makeText(getApplicationContext(), R.string.main_activity_message_its_not_a_image, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent();
                    Bundle args = new Bundle();
                    args.putParcelable(MyConst.FILE_ITEM, list.get(i));
                    args.putParcelable(MyConst.CREDENTIALS, credentials);
                    args.putParcelableArrayList(MyConst.LIST,list);
                    intent.putExtras(args);
                    intent.setClass(getApplicationContext(), ViewPictures.class);
                    startActivity(intent);


                    return;
                }
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                intent.putExtra(MyConst.DIRECTORY_PATH, list.get(i).getFullPath());
                startActivity(intent);
            }
        });

        Button buttonShowPictures = (Button) findViewById(R.id.mainActivityButtonShow);

        buttonShowPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ListItem listItem = null;
                for (ListItem item : list) {
                    if (item.getContentType() != null && item.getContentType().indexOf("image") == 0) {
                        listItem = item;
                        break;
                    }
                }
                if (listItem == null) {
                    Toast.makeText(getApplicationContext(), R.string.no_images, Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent();
                    Bundle args = new Bundle();
                    args.putParcelable(MyConst.FILE_ITEM, listItem);
                    args.putParcelable(MyConst.CREDENTIALS, credentials);
                    args.putParcelableArrayList(MyConst.LIST, list);
                    intent.putExtra(MyConst.SLIDE_SHOW, true);
                    intent.putExtras(args);
                    intent.setClass(getApplicationContext(), ViewPictures.class);
                    startActivity(intent);
                }

            }
        });


        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((Button) findViewById(R.id.mainActivityButtonShow)).setEnabled(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
        ((Button) findViewById(R.id.mainActivityButtonShow)).setEnabled(false);
        return new MyLoader(getApplicationContext(), credentials, directoryPath);
    }

    @Override
    public void onLoadFinished(Loader<List<ListItem>> listLoader, List<ListItem> listItems) {
        if (listItems.isEmpty()) {
            Toast.makeText(getApplicationContext(),R.string.main_activity_folder_is_emtpy,Toast.LENGTH_SHORT).show();
        } else {
            ((Button) findViewById(R.id.mainActivityButtonShow)).setEnabled(true);
            arrayAdapter.clear();
            list.clear();
            arrayAdapter.addAll(listItems);
            list.addAll(listItems);
            listView.setAdapter(arrayAdapter);
        }

    }

    @Override
    public void onLoaderReset(Loader<List<ListItem>> listLoader) {
        arrayAdapter.clear();
    }
}
