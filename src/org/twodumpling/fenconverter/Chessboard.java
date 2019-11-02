/*
 * Copyright 2013 Istvan Chung and Husayn Karimi.
 *
 * This file is part of ChessSave.
 *
 * ChessSave is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ChessSave is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChessSave.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.twodumpling.fenconverter;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class Chessboard extends ImageView {
	private static final Map<Character, Character> PIECE_DISPLAY;

	static {
		PIECE_DISPLAY = new HashMap<Character, Character>(13);
		PIECE_DISPLAY.put('0', ' ');
		// Use "black" glyphs as they are filled; use Paint for color instead
		PIECE_DISPLAY.put('K', '\u265A');
		PIECE_DISPLAY.put('Q', '\u265B');
		PIECE_DISPLAY.put('R', '\u265C');
		PIECE_DISPLAY.put('B', '\u265D');
		PIECE_DISPLAY.put('N', '\u265E');
		PIECE_DISPLAY.put('P', '\u265F');
		PIECE_DISPLAY.put('k', '\u265A');
		PIECE_DISPLAY.put('q', '\u265B');
		PIECE_DISPLAY.put('r', '\u265C');
		PIECE_DISPLAY.put('b', '\u265D');
		PIECE_DISPLAY.put('n', '\u265E');
		PIECE_DISPLAY.put('p', '\u265F');
	}

	private MainActivity main = null;
	private Paint whitePiece, blackPiece;

	{
		Paint piecePaint = new Paint();
		piecePaint.setTextAlign(Paint.Align.CENTER);
		// Don't try to load font in eclipse's preview.
		if (!isInEditMode()) {
			piecePaint.setTypeface(Typeface.createFromAsset(getContext()
					.getAssets(), "fonts/FreeSerif.ttf"));
		}
		piecePaint.setAntiAlias(true);
		whitePiece = new Paint(piecePaint);
		blackPiece = new Paint(piecePaint);
		whitePiece.setColor(0xffffffff);
		blackPiece.setColor(0xff000000);
	}

	public Chessboard(Context context) {
		super(context);
	}

	public Chessboard(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Chessboard(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	protected void registerMainActivity(MainActivity main) {
		this.main = main;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (isInEditMode()) {
			// Don't try to display pieces in eclipse's preview
			return;
		}

		char[][] pieces = main.getPieces();

		// Get the bounds of the chessboard image
		RectF drawableRect = new RectF(getDrawable().getBounds());
		getImageMatrix().mapRect(drawableRect);
		float w = drawableRect.width(), h = drawableRect.height();

		whitePiece.setTextSize(w / pieces[0].length);
		blackPiece.setTextSize(w / pieces[0].length);

		for (int row = 0; row < pieces.length; row++) {
			for (int column = 0; column < pieces[row].length; column++) {
				String text = Character.toString(PIECE_DISPLAY
						.get(pieces[row][column]));
				float x = column * w / pieces[row].length + w
						/ pieces[row].length / 2 + drawableRect.left;
				// Baseline 5/6 of the way down by trial and error
				float y = row * h / pieces.length + h * 5 / 6 / pieces.length
						+ drawableRect.top;
				canvas.drawText(text, x, y, Character
						.isUpperCase(pieces[row][column]) ? whitePiece
						: blackPiece);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (main != null) {
			int x = (int) event.getX(), y = (int) event.getY();
			RectF drawableRect = new RectF(getDrawable().getBounds());
			getImageMatrix().mapRect(drawableRect);
			x -= drawableRect.left;
			y -= drawableRect.top;
			int w = (int) drawableRect.width(), h = (int) drawableRect.height();
			int row = y * 8 / h, column = x * 8 / w;
			main.boardClick(row, column);
		}

		return super.onTouchEvent(event);
	}
}
