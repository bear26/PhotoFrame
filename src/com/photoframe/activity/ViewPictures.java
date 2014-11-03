package com.photoframe.activity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.photoframe.util.MyConst;
import com.photoframe.R;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class ViewPictures extends Activity {
    private ListItem item;
    private Credentials credentials;
    private Handler handler;
    private DownloadingFile workFragment;
    private final long TIME_WATCHING = 1000 * 7;
    private String lastName = "";
    private boolean isSlideShow;
    private File oldFile;
    private long oldTimeSetImage;
    private ArrayList<ListItem> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pictures);
        credentials = getIntent().getExtras().getParcelable(MyConst.CREDENTIALS);
        handler = new Handler();
        workFragment = new DownloadingFile();
        item = getIntent().getExtras().getParcelable(MyConst.FILE_ITEM);
        isSlideShow = getIntent().getExtras().getBoolean(MyConst.SLIDE_SHOW, false);

        list = getIntent().getExtras().getParcelableArrayList(MyConst.LIST);

        Button buttonPrevious = (Button) findViewById(R.id.viewPicturesButtonPrevious);
        Button buttonNext = (Button) findViewById(R.id.viewPicturesButtonNext);
        oldFile = null;
        oldTimeSetImage = 0;
        if (isSlideShow) {
            buttonNext.setVisibility(View.GONE);
            buttonPrevious.setVisibility(View.GONE);
        } else {
            buttonNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!(item.getName().equals(getNextItem(item).getName()) && item.getLastUpdated() == getNextItem(item).getLastUpdated())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                lastName = item.getDisplayName();
                                item = getNextItem(item);
                                download();

                            }
                        });
                        buttonNext.setEnabled(false);
                        buttonPrevious.setEnabled(false);

                    } else
                        Toast.makeText(getApplicationContext(),R.string.view_pictures_folder_has_one_image,Toast.LENGTH_SHORT).show();
                }
            });
            buttonPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!(item.getName().equals(getNextItem(item).getName()) && item.getLastUpdated() == getNextItem(item).getLastUpdated())) {

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                lastName = item.getDisplayName();
                                item = getPreviousItem(item);
                                download();

                            }
                        });
                        buttonPrevious.setEnabled(false);
                        buttonNext.setEnabled(false);
                    }else
                        Toast.makeText(getApplicationContext(),R.string.view_pictures_folder_has_one_image,Toast.LENGTH_SHORT).show();
                }
            });
        }

        download();
    }


    private void download() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setDownloadingProgress(0, 1);
            }
        });

        workFragment.loadFIle(getApplicationContext(), credentials, item);
    }

    private void setImage(File file) {

        TextView textView = (TextView) findViewById(R.id.viewPicturesPercentTextFirstView);
        textView.setText(item.getDisplayName());

        if (oldFile != null) {
            deleteFile(oldFile.getName());
        }
        oldFile = file;


        oldTimeSetImage = System.currentTimeMillis();
        ImageView imageView = (ImageView) findViewById(R.id.viewPicturesImageFirstView);
        imageView.setImageURI(Uri.fromFile(file));

        if (isSlideShow) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!(item.getName().equals(getNextItem(item).getName()) && item.getLastUpdated() == getNextItem(item).getLastUpdated())) {
                        lastName = item.getDisplayName();
                        item = getNextItem(item);

                        download();
                    }
                }
            });
        }

    }

    private ListItem getNextItem(ListItem listItem) {

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getFullPath().equals(listItem.getFullPath())) {
                for (int j = i + 1; j < list.size(); j++) {
                    if (isImage(list.get(j))) return list.get(j);
                }
                for (int j = 0; j < i; j++) {
                    if (isImage(list.get(j))) return list.get(j);
                }

                break;
            }
        }
        return listItem;
    }

    private ListItem getPreviousItem(ListItem listItem) {

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getFullPath().equals(listItem.getFullPath())) {
                for (int j = i - 1; j >= 0; j--) {
                    if (isImage(list.get(j))) return list.get(j);
                }
                for (int j = list.size() - 1; j > i; j--) {
                    if (isImage(list.get(j))) return list.get(j);
                }
                break;
            }
        }
        return listItem;
    }

    private boolean isImage(ListItem listItem) {
        if (listItem.getContentType() == null || listItem.getContentType().indexOf("image") != 0) return false;
        return true;

    }

    private void onDownloadingComplete(File file) {

        long delay;
        if (System.currentTimeMillis() - oldTimeSetImage >= TIME_WATCHING || !isSlideShow) delay = 0;
        else
            delay = TIME_WATCHING - (System.currentTimeMillis() - oldTimeSetImage);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setImage(file);
            }
        }, delay);
        ((Button) findViewById(R.id.viewPicturesButtonPrevious)).setEnabled(true);
        ((Button) findViewById(R.id.viewPicturesButtonNext)).setEnabled(true);

    }


    private void setDownloadingProgress(long loader, long total) {
        if (!isSlideShow || lastName.length() == 0) {
            TextView textView = (TextView) findViewById(R.id.viewPicturesPercentTextFirstView);
            textView.setText(((lastName.length() > 0) ? (lastName + "/") : "") + item.getDisplayName() + "(" + (loader * 100 / total) + "%" + ")");

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workFragment != null)
            workFragment.cancelDownload();
        if (oldFile != null) {
            deleteFile(oldFile.getName());
        }
    }

    private class DownloadingFile implements ProgressListener {
        private File result;
        private boolean cancelled;

        public void loadFIle(final Context context, final Credentials credentials, final ListItem item) {
            result = new File(context.getFilesDir(), new File(item.getFullPath()).getName());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    TransportClient client = null;
                    try {
                        client = TransportClient.getInstance(context, credentials);
                        client.downloadFile(item.getFullPath(), result, DownloadingFile.this);
                        downloadComplete();
                    } catch (WebdavException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();

                    } finally {
                        if (client != null)
                            client.shutdown();
                    }

                }
            }).start();
        }


        public void downloadComplete() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onDownloadingComplete(result);
                }
            });
        }

        @Override
        public void updateProgress(final long loaded, final long total) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setDownloadingProgress(loaded, total);
                }
            });
        }

        @Override
        public boolean hasCancelled() {
            return cancelled;
        }

        public void cancelDownload() {
            cancelled = true;
        }
    }


}
