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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Creates, imports, and exports FEN chess position files.
 * 
 * @author Istvan Chung
 */
public class MainActivity extends Activity {
	/**
	 * Unpack an FEN string into an array.
	 * 
	 * @param fen
	 *            the FEN string
	 * @return See {@link #getPieces()}
	 */
	public static char[][] fenToCharArr(String fen) {
		char[][] board = new char[8][8];
		String fenStringRows = fen.substring(0, fen.indexOf(" "));
		String[] rows = fenStringRows.split("/");
		if (rows.length != 8) {
			throw new IllegalArgumentException("Wrong number of rows");
		}
		for (int i = 0; i < rows.length; i++) {
			int column = 0;
			for (int y = 0; y < rows[i].length(); y++) {
				char c = rows[i].charAt(y);
				if (c >= '0' && c <= '9') {
					for (int z = 0; z < c - '0'; z++) {
						board[i][column] = '0';
						column++;
					}
				}
				else if ("KQRBNPkqrbnp".indexOf(c) != -1) {
					board[i][column] = rows[i].charAt(y);
					column++;
				}
				else {
					throw new IllegalArgumentException("Invalid character");
				}
			}
			if (column != 8) {
				throw new IllegalArgumentException("Row wrong length");
			}
		}
		return board;
	}

	/**
	 * Create an FEN string.
	 * 
	 * @param board
	 *            an array representation, described in {@link #getPieces()}
	 * @param whiteToMove
	 *            whether white is to move
	 * @return an FEN string with the specified position and to-move color
	 */
	public static String charToFen(char[][] board, boolean whiteToMove) {
		StringBuilder fenBuilder = new StringBuilder();
		for (int i = 0; i < board.length; i++) {
			int blank = 0;
			for (int y = 0; y < board[0].length; y++) {
				if (board[i][y] == '0') {
					blank++;
				}
				else {
					if (blank > 0) {
						fenBuilder.append(blank);
						fenBuilder.append(board[i][y]);
					}
					else {
						fenBuilder.append(board[i][y]);
					}
					blank = 0;
				}
			}
			if (blank > 0) {
				fenBuilder.append(blank);
			}
			if (i != board.length - 1) {
				fenBuilder.append("/");
			}
		}
		fenBuilder.append(" ");
		fenBuilder.append(whiteToMove ? "w" : "b");
		fenBuilder.append(" - - 0 1");
		return fenBuilder.toString();
	}

	/**
	 * Determine who is to move from an FEN string.
	 * 
	 * @param fen
	 *            the FEN string
	 * @return {@code true} if white is to move, {@code false} otherwise.
	 */
	public static boolean fenToMove(String fen) {
		return (fen.substring(fen.indexOf(" "), fen.length() - 1)).substring(1,
				2).equals("w");
	}

	/**
	 * {@code true} iff white is to move
	 */
	private boolean whiteToMove;
	private Chessboard chessboard;
	private Map<Button, Character> pieceButtons;
	private char selectedPiece;
	private Button selectedButton;
	private ColorStateList oldColor;
	/**
	 * See {@link #getPieces()}
	 */
	private char[][] pieces;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		chessboard = (Chessboard) findViewById(R.id.chessboard);
		chessboard.registerMainActivity(this);

		pieceButtons = new HashMap<Button, Character>(13);
		pieceButtons.put((Button) findViewById(R.id.whiteKing), 'K');
		pieceButtons.put((Button) findViewById(R.id.whiteQueen), 'Q');
		pieceButtons.put((Button) findViewById(R.id.whiteRook), 'R');
		pieceButtons.put((Button) findViewById(R.id.whiteBishop), 'B');
		pieceButtons.put((Button) findViewById(R.id.whiteKnight), 'N');
		pieceButtons.put((Button) findViewById(R.id.whitePawn), 'P');
		pieceButtons.put((Button) findViewById(R.id.blackKing), 'k');
		pieceButtons.put((Button) findViewById(R.id.blackQueen), 'q');
		pieceButtons.put((Button) findViewById(R.id.blackRook), 'r');
		pieceButtons.put((Button) findViewById(R.id.blackBishop), 'b');
		pieceButtons.put((Button) findViewById(R.id.blackKnight), 'n');
		pieceButtons.put((Button) findViewById(R.id.blackPawn), 'p');

		Typeface typeface = Typeface.createFromAsset(getAssets(),
				"fonts/FreeSerif.ttf");
		for (Button b : pieceButtons.keySet()) {
			b.setTypeface(typeface);
			b.setTextSize(30);
		}

		// Avoid using FreeSerif for remove button.
		pieceButtons.put((Button) findViewById(R.id.removePiece), '0');

		if (savedInstanceState == null) {
			clearBoard();
			whiteToMove = true;
			selectedButton = null;
			selectedPiece = '0';
		}
		else {
			String fen = savedInstanceState.getString("FEN");

			try {
				loadFEN(fen);
			}
			catch (IllegalArgumentException e) {
				Log.d("MainActivity", "Load FEN failed", e);
				clearBoard();
				whiteToMove = true;
			}

			Button oldSelectedButton = (Button) findViewById(savedInstanceState.getInt(
					"selectedButtonId", View.NO_ID));
			if (oldSelectedButton == null) {
				selectedPiece = '0';
			}
			else {
				onPieceButtonClicked(oldSelectedButton);
			}
		}
	}

	/**
	 * Load an FEN string.
	 * 
	 * @throws IllegalArgumentException
	 *             if the FEN string is malformed
	 * @param fen
	 *            the FEN string
	 */
	@SuppressLint("NewApi")
	private void loadFEN(String fen) {
		char[][] oldPieces = pieces;
		boolean oldWhiteToMove = whiteToMove;
		try {
			pieces = fenToCharArr(fen);
			whiteToMove = fenToMove(fen);
			View toMoveSwitch = findViewById(R.id.tomove);
			if (toMoveSwitch != null) {
				((Switch) toMoveSwitch).setChecked(whiteToMove);
			}
			else {
				RadioButton toMoveButton = (RadioButton) findViewById(whiteToMove ? R.id.whitemove
						: R.id.blackmove);
				toMoveButton.setChecked(true);
			}
			chessboard.invalidate();
		}
		catch (Exception e) {
			pieces = oldPieces;
			whiteToMove = oldWhiteToMove;
			throw new IllegalArgumentException("Malformed FEN", e);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		if (selectedButton != null) {
			savedInstanceState.putInt("selectedButtonId",
					selectedButton.getId());
		}
		savedInstanceState.putString("FEN", charToFen(pieces, whiteToMove));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void boardClick(int row, int column) {
		if (selectedPiece != 0 && row >= 0 && row < pieces.length
				&& column >= 0 && column < pieces[row].length) {
			if (pieces[row][column] == selectedPiece) {
				// Clear by clicking on the same piece
				pieces[row][column] = '0';
			}
			else {
				pieces[row][column] = selectedPiece;
			}
			chessboard.invalidate();
		}
	}

	public void onWhiteClicked(View v) {
		whiteToMove = true;
	}

	public void onBlackClicked(View v) {
		whiteToMove = false;
	}

	@SuppressLint("NewApi")
	public void onToMoveSwitched(View v) {
		whiteToMove = ((Switch) v).isChecked();
	}

	/**
	 * Get a clone of the pieces on the board.
	 * 
	 * @return A two-dimensional row-major order array of {@code char}s, using
	 *         the same representation as FEN for pieces and {@code '0'} for a
	 *         blank square.
	 */
	public char[][] getPieces() {
		char[][] pieces = new char[this.pieces.length][];
		for (int i = 0; i < pieces.length; i++) {
			pieces[i] = this.pieces[i].clone();
		}

		return pieces;
	}

	public void onPieceButtonClicked(View v) {
		if (v == selectedButton) {
			return;
		}

		Character c = pieceButtons.get(v);
		if (c == null) {
			throw new IllegalArgumentException("Button not found in mapping.");
		}

		if (selectedButton != null && oldColor != null) {
			// Reset the color of newly deselected button
			selectedButton.setTextColor(oldColor);
		}

		selectedPiece = c;
		selectedButton = (Button) v;
		// Retain the color to reset it when this button becomes deselected
		oldColor = selectedButton.getTextColors();
		selectedButton.setTextColor(0xffcccc33);
	}

	private static final Pattern indexPattern = Pattern.compile("^(.*):(.*)$");

	/**
	 * Read the index file.
	 * 
	 * @return A {@code Map}ping from user-readable game names to filenames.
	 * @throws IOException
	 */
	private Map<String, String> readIndex() throws IOException {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					openFileInput("index")));
			Map<String, String> acc = new HashMap<String, String>();
			String line;
			while ((line = in.readLine()) != null) {
				Matcher matcher = indexPattern.matcher(line);
				matcher.matches();
				acc.put(matcher.group(1), matcher.group(2));
			}
			in.close();
			return acc;
		}
		catch (FileNotFoundException e) {
			return new HashMap<String, String>(0);
		}
	}

	/**
	 * Write the index file.
	 * 
	 * @param index
	 *            a {@code Map}ping from game names to filenames
	 * @throws IOException
	 */
	private void writeIndex(Map<String, String> index) throws IOException {
		PrintWriter out = new PrintWriter(openFileOutput("index",
				Context.MODE_PRIVATE));
		for (Map.Entry<String, String> e : index.entrySet()) {
			out.print(e.getKey());
			out.print(':');
			out.println(e.getValue());
		}
		out.close();
		if (out.checkError()) {
			throw new IOException();
		}
	}

	private class ImportDialog extends AlertDialog {
		protected ImportDialog() {
			super(MainActivity.this);

			setTitle(R.string.import_position);

			LayoutInflater layoutInflater = MainActivity.this
					.getLayoutInflater();
			ViewGroup view = (ViewGroup) layoutInflater.inflate(
					R.layout.import_alert, null);

			final EditText input = (EditText) view.findViewById(R.id.pasteFEN);
			input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// Necessary to clear error hint when user types, contrary
					// to the documentation on TextView.setError(CharSequence).
					input.setError(null);
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});
			Button pasteFENButton = (Button) view
					.findViewById(R.id.pasteFENButton);
			pasteFENButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String fen = input.getText().toString();
					try {
						loadFEN(fen);
						Toast.makeText(MainActivity.this, R.string.imported,
								Toast.LENGTH_SHORT).show();
						dismiss();
					}
					catch (IllegalArgumentException e) {
						// Notify the user of illegal FEN.
						input.setError(getText(R.string.invalid_fen));
					}
				}
			});

			try {
				final Map<String, String> index = readIndex();
				// Only show import file dialog if there are files to import
				if (index != null && !index.isEmpty()) {
					View fileView = layoutInflater.inflate(
							R.layout.import_file, null);

					final Spinner chooseFile = (Spinner) fileView
							.findViewById(R.id.chooseFile);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							MainActivity.this,
							android.R.layout.simple_spinner_item,
							new ArrayList<String>(index.keySet()));
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					chooseFile.setAdapter(adapter);

					Button importFileButton = (Button) fileView
							.findViewById(R.id.importFileButton);
					importFileButton
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									String name = chooseFile.getSelectedItem()
											.toString();
									try {
										String filename = index.get(name);
										if (filename == null) {
											throw new FileNotFoundException(
													name + ":" + filename);
										}
										BufferedReader in = new BufferedReader(
												new InputStreamReader(
														openFileInput(filename)));
										String fen = in.readLine();
										loadFEN(fen);
										Toast.makeText(
												MainActivity.this,
												String.format(
														getText(
																R.string.f_imported)
																.toString(),
														name),
												Toast.LENGTH_SHORT).show();
										dismiss();
									}
									catch (FileNotFoundException e) {
										if (index.remove(name) != null) {
											try {
												writeIndex(index);
											}
											catch (IOException e1) {
												throw new RuntimeException(e1);
											}
										}
										throw new RuntimeException(e);
									}
									catch (IOException e) {
										throw new RuntimeException(e);
									}
									catch (IllegalArgumentException e) {
										if (index.remove(name) != null) {
											try {
												writeIndex(index);
											}
											catch (IOException e1) {
												throw new RuntimeException(e1);
											}
										}
										throw e;
									}
								}
							});

					view.addView(fileView);
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}

			setView(view);

			setButton(BUTTON_NEGATIVE, getText(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// No action required.
						}
					});
		}
	}

	private class ExportDialog extends AlertDialog {
		protected ExportDialog() {
			super(MainActivity.this);

			setTitle(R.string.export_position);

			final String fen = charToFen(pieces, whiteToMove);
			LayoutInflater layoutInflater = MainActivity.this
					.getLayoutInflater();
			View view = layoutInflater.inflate(R.layout.export_alert, null);

			TextView displayFEN = (TextView) view.findViewById(R.id.displayFEN);
			displayFEN.setText(fen);

			Button copyButton = (Button) view.findViewById(R.id.copyFENButton);
			copyButton.setOnClickListener(new View.OnClickListener() {
				@SuppressWarnings("deprecation")
				@SuppressLint("NewApi")
				@Override
				public void onClick(View v) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						clipboard.setText(fen);
					}
					else {
						android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						android.content.ClipData clip = android.content.ClipData
								.newPlainText("FEN", fen);
						clipboard.setPrimaryClip(clip);
					}

					Toast.makeText(getBaseContext(),
							R.string.text_copied_to_clipboard,
							Toast.LENGTH_SHORT).show();

					dismiss();
				}
			});

			final EditText editFilename = (EditText) view
					.findViewById(R.id.editFilename);
			final Date date = new Date();
			// Format for default entry for name
			DateFormat userFormat = DateFormat.getDateTimeInstance();
			editFilename.setText(userFormat.format(date));

			Button exportFile = (Button) view
					.findViewById(R.id.exportFileButton);
			exportFile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Map<String, String> index = readIndex();
						String name = editFilename.getText().toString();
						String filename = index.get(name);
						if (filename == null) {
							// Format for filename
							DateFormat fileFormat = new SimpleDateFormat(
									"yyyy mm dd  HH mm ss'.fen'", Locale.US);
							// Use timestamp to (hopefully) create
							// nonconflicting filenames.
							filename = fileFormat.format(date);
							index.put(name, filename);
							writeIndex(index);
						}
						PrintWriter out = new PrintWriter(openFileOutput(
								filename, Context.MODE_PRIVATE));
						out.println(charToFen(pieces, whiteToMove));
						out.close();
						Toast.makeText(
								MainActivity.this,
								String.format(getText(R.string.f_saved)
										.toString(), name), Toast.LENGTH_SHORT)
								.show();
						dismiss();
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});

			setView(view);

			setButton(BUTTON_NEGATIVE, getText(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// No action required.
						}
					});
		}
	}

	public void onImportClicked(View v) {
		AlertDialog alert = new ImportDialog();
		alert.show();
	}

	public void onExportClicked(View v) {
		AlertDialog alert = new ExportDialog();
		alert.show();
	}

	private void clearBoard() {
		pieces = new char[8][8];
		for (int i = 0; i < pieces.length; i++) {
			for (int j = 0; j < pieces[i].length; j++) {
				pieces[i][j] = '0';
			}
		}
		chessboard.invalidate();
	}

	private class DeleteFileDialog extends AlertDialog {
		private boolean preventShow = false;

		protected DeleteFileDialog() {
			super(MainActivity.this);

			setTitle(R.string.delete_saved);

			LayoutInflater layoutInflater = MainActivity.this
					.getLayoutInflater();
			ViewGroup view = (ViewGroup) layoutInflater.inflate(
					R.layout.delete_file, null);

			try {
				final Map<String, String> index = readIndex();
				if (!index.isEmpty()) {
					final Spinner chooseFile = (Spinner) view
							.findViewById(R.id.chooseDeleteFile);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							MainActivity.this,
							android.R.layout.simple_spinner_item,
							new ArrayList<String>(index.keySet()));
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					chooseFile.setAdapter(adapter);

					Button deleteFileButton = (Button) view
							.findViewById(R.id.deleteFileButton);
					deleteFileButton
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									try {
										String name = chooseFile
												.getSelectedItem().toString();
										String filename = index.get(name);
										if (filename == null) {
											throw new FileNotFoundException(
													name + ":" + filename);
										}
										index.remove(name);
										writeIndex(index);
										deleteFile(filename);
										Toast.makeText(
												MainActivity.this,
												String.format(
														getText(
																R.string.f_deleted)
																.toString(),
														name),
												Toast.LENGTH_SHORT).show();
										dismiss();
									}
									catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
							});
				}
				else {
					// If there are no files to delete, notify user.
					Toast.makeText(MainActivity.this, R.string.no_saved_files,
							Toast.LENGTH_SHORT).show();
					preventShow = true;
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}

			setView(view);

			setButton(BUTTON_NEGATIVE, getText(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// No action required.
						}
					});
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.clear_board:
				clearBoard();
				return true;
			case R.id.delete_saved:
				DeleteFileDialog alert = new DeleteFileDialog();
				if (!alert.preventShow) {
					alert.show();
				}
				return true;
			case R.id.about:
				// Show license and absence of warranty.
				AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
				aboutDialog.setTitle(R.string.about);
				LayoutInflater layoutInflater = getLayoutInflater();
				View view = layoutInflater.inflate(R.layout.about, null);
				aboutDialog.setView(view);
				aboutDialog.setPositiveButton(R.string.show_license,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								AlertDialog.Builder licenseDialog = new AlertDialog.Builder(
										MainActivity.this);
								licenseDialog.setTitle(R.string.license);
								WebView webView = new WebView(MainActivity.this);
								webView.loadUrl("file:///android_asset/gpl-3.0-standalone.html");
								licenseDialog.setView(webView);
								licenseDialog.setNegativeButton(R.string.done,
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface arg0,
													int arg1) {
												// No action required.
											}
										});
								licenseDialog.show();
							}
						});
				aboutDialog.setNegativeButton(R.string.done,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// No action required.
							}
						});
				aboutDialog.show();
				return true;
			case R.id.setup_board:
				loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
