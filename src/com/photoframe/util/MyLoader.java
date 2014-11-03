package com.photoframe.util;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Handler;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ListParsingHandler;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.*;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyLoader extends AsyncTaskLoader<List<ListItem>> {
    private Credentials credentials;
    private String dir;
    private Handler handler;
    private static final int ITEMS_PER_REQUEST = 20;
    private boolean hasCancelled;

    public MyLoader(Context context, Credentials credentials, String dir) {
        super(context);
        this.credentials = credentials;
        this.dir = dir;
        handler = new Handler();
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        hasCancelled = true;
    }

    @Override
    public List<ListItem> loadInBackground() {
        final List<ListItem> list = new ArrayList<ListItem>();
        hasCancelled = false;
        TransportClient client = null;
        try {
            client = TransportClient.getInstance(getContext(), credentials);
            client.getList(dir, ITEMS_PER_REQUEST, new ListParsingHandler() {

                boolean ignoreFirstItem = true;

                @Override
                public boolean hasCancelled() {
                    return hasCancelled;
                }

                @Override
                public void onPageFinished(int itemsOnPage) {
                    ignoreFirstItem = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Collections.sort(list, new Comparator<ListItem>() {
                                private Collator collator = Collator.getInstance();

                                {
                                    collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
                                }

                                @Override
                                public int compare(ListItem f1, ListItem f2) {
                                    if (f1.isCollection() && !f2.isCollection()) {
                                        return -1;
                                    } else if (f2.isCollection() && !f1.isCollection()) {
                                        return 1;
                                    } else {
                                        return collator.compare(f1.getDisplayName(), f2.getDisplayName());
                                    }
                                }
                            });
                            deliverResult(new ArrayList<ListItem>(list));
                        }
                    });
                }

                @Override
                public boolean handleItem(ListItem item) {
                    if (ignoreFirstItem) {
                        ignoreFirstItem = false;
                        return false;
                    } else {
                        list.add(item);
                        return true;
                    }

                }
            });
        } catch (WebdavException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

            return list;
    }
}
