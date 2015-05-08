/*
 * Copyright 2013 Evelio Tarazona Cáceres <evelio@evelio.info>
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
package info.guardianproject.otr.app.im.ui;

import info.guardianproject.otr.app.im.app.ImApp;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * A Drawable that draws an oval with given {@link Bitmap}
 */
public class RoundedAvatarDrawable extends Drawable {
  private final Bitmap mBitmap;
  private final Paint mPaint;
  private final RectF mRectF;
  private final int mBitmapWidth;
  private final int mBitmapHeight;

  private Paint mPaintBorder;
  private int mBorderWidth = 4;

  public RoundedAvatarDrawable(Bitmap bitmap) {
    mBitmap = bitmap;
    mRectF = new RectF();
    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);

    BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    mPaint.setShader(shader);

    // NOTE: we assume bitmap is properly scaled to current density
    mBitmapWidth = mBitmap.getWidth();
    mBitmapHeight = mBitmap.getHeight();

    mPaintBorder = new Paint();
    mPaintBorder.setColor(Color.LTGRAY);
    mPaintBorder.setStyle(Paint.Style.STROKE);
    mPaintBorder.setAntiAlias(true);
    mPaintBorder.setStrokeWidth(mBorderWidth);

  }

  public void setBorderColor (int borderColor)
  {
      mPaintBorder.setColor(borderColor);
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawOval(mRectF, mPaint);

    float halfWidth = mRectF.width()/2;
    canvas.drawCircle(halfWidth, halfWidth, halfWidth-mBorderWidth/2, mPaintBorder);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    mRectF.set(bounds);
  }

  @Override
  public void setAlpha(int alpha) {
    if (mPaint.getAlpha() != alpha) {
      mPaint.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public int getIntrinsicWidth() {
    return mBitmapWidth;
  }

  @Override
  public int getIntrinsicHeight() {
    return mBitmapHeight;
  }

  public void setAntiAlias(boolean aa) {
    mPaint.setAntiAlias(aa);
    invalidateSelf();
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    mPaint.setFilterBitmap(filter);
    invalidateSelf();
  }

  @Override
  public void setDither(boolean dither) {
    mPaint.setDither(dither);
    invalidateSelf();
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  // TODO allow set and use target density, mutate, constant state, changing configurations, etc.
}