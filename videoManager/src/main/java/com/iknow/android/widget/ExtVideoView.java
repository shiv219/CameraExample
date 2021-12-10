package com.iknow.android.widget;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * author : J.Chou
 * e-mail : who_know_me@163.com
 * time   : 2018/10/20 11:22 AM
 * version: 1.0
 * description:
 */
public class ExtVideoView extends VideoView {
  private int mVideoWidth = 480;
  private int mVideoHeight = 480;
  private int videoRealW = 1;
  private int videoRealH = 1;

  public ExtVideoView(Context context) {
    super(context);
  }

  public ExtVideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ExtVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void setVideoURI(Uri uri) {
    super.setVideoURI(uri);
    MediaMetadataRetriever retr = new MediaMetadataRetriever();
    retr.setDataSource(uri.getPath());
    String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
    String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
    try {
      videoRealH = Integer.parseInt(height);
      videoRealW = Integer.parseInt(width);
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

  }

}
