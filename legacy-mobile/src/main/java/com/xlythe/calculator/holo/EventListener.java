/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.xlythe.calculator.holo;

import android.content.Context;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xlythe.calculator.holo.Page.NormalPanel;
import com.xlythe.calculator.holo.view.CalculatorViewPager;
import com.xlythe.calculator.holo.view.GraphView;
import com.xlythe.calculator.holo.view.MatrixEditText;
import com.xlythe.calculator.holo.view.MatrixInverseView;
import com.xlythe.calculator.holo.view.MatrixTransposeView;
import com.xlythe.calculator.holo.view.MatrixView;
import com.xlythe.math.Base;

import com.xlythe.math.SyntaxException;

public class EventListener implements View.OnKeyListener, View.OnClickListener, View.OnLongClickListener {
    Context mContext;
    Logic mHandler;
    CalculatorViewPager mPager;
    CalculatorViewPager mSmallPager;
    CalculatorViewPager mLargePager;
    GraphView mGraphDisplay;

    private String mErrorString;
    private String mModString;
    private String mX;
    private String mY;
    private String mDX;
    private String mDY;

    void setHandler(Context context, Logic handler, CalculatorViewPager pager) {
        setHandler(context, handler, pager, null, null);
    }

    private void setHandler(Context context, Logic handler, CalculatorViewPager pager,
                            CalculatorViewPager smallPager, CalculatorViewPager largePager) {
        mContext = context;
        mHandler = handler;
        mPager = pager;
        mSmallPager = smallPager;
        mLargePager = largePager;

        mErrorString = mContext.getString(R.string.error);
        mModString = mContext.getString(R.string.mod);
        mX = mContext.getString(R.string.X);
        mY = mContext.getString(R.string.Y);
        mDX = mContext.getString(R.string.dx);
        mDY = mContext.getString(R.string.dy);
    }

    void setHandler(Context context, Logic handler, CalculatorViewPager smallPager,
                    CalculatorViewPager largePager) {
        setHandler(context, handler, null, smallPager, largePager);
    }

    @Override
    public void onClick(View view) {
        vibrate();
        View v;
        EditText active;
        int id = view.getId();
        switch (id) {
            case R.id.del:
                mHandler.onDelete();
                break;

            case R.id.clear:
                mHandler.onClear();
                break;

            case R.id.equal:
                if (mHandler.getText().contains(mX) || mHandler.getText().contains(mY)) {
                    if (!mHandler.getText().contains("=")) {
                        mHandler.insert("=");
                        returnToBasic();
                    }
                    break;
                }
                mHandler.onEnter();
                break;

            case R.id.hex:
                try {
                    mHandler.setText(mHandler.getBaseModule().setBase(mHandler.getText().toString(),
                            Base.HEXADECIMAL));
                } catch (SyntaxException e) {
                    e.printStackTrace();
                }
                applyBannedResources(Base.HEXADECIMAL);
                break;

            case R.id.bin:
                try {
                    mHandler.setText(mHandler.getBaseModule().setBase(mHandler.getText().toString(),
                            Base.BINARY));
                } catch (SyntaxException e) {
                    e.printStackTrace();
                }
                applyBannedResources(Base.BINARY);
                break;

            case R.id.dec:
                try {
                    mHandler.setText(mHandler.getBaseModule().setBase(mHandler.getText().toString(),
                            Base.DECIMAL));
                } catch (SyntaxException e) {
                    e.printStackTrace();
                }
                applyBannedResources(Base.DECIMAL);
                break;

            case R.id.matrix:
                mHandler.insert(MatrixView.getPattern());
                returnToBasic();
                break;

            case R.id.matrix_inverse:
                mHandler.insert(MatrixInverseView.PATTERN);
                returnToBasic();
                break;

            case R.id.matrix_transpose:
                mHandler.insert(MatrixTransposeView.PATTERN);
                returnToBasic();
                break;

            case R.id.plus_row:
                v = mHandler.mDisplay.getActiveEditText();
                if (v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().addRow();
                break;

            case R.id.minus_row:
                v = mHandler.mDisplay.getActiveEditText();
                if (v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().removeRow();
                break;

            case R.id.plus_col:
                v = mHandler.mDisplay.getActiveEditText();
                if (v instanceof MatrixEditText) ((MatrixEditText) v).getMatrixView().addColumn();
                break;

            case R.id.minus_col:
                v = mHandler.mDisplay.getActiveEditText();
                if (v instanceof MatrixEditText)
                    ((MatrixEditText) v).getMatrixView().removeColumn();
                break;

            case R.id.next:
                if (mHandler.getText().equals(mErrorString)) mHandler.setText("");
                active = mHandler.mDisplay.getActiveEditText();
                if (active.getSelectionStart() == active.getText().length()) {
                    v = mHandler.mDisplay.getActiveEditText().focusSearch(View.FOCUS_FORWARD);
                    if (v != null) v.requestFocus();
                    active = mHandler.mDisplay.getActiveEditText();
                    active.setSelection(0);
                } else {
                    active.setSelection(active.getSelectionStart() + 1);
                }
                break;

            case R.id.parentheses:
                if (mHandler.getText().equals(mErrorString)) mHandler.setText("");
                if (mHandler.getText().contains("=")) {
                    String[] equation = mHandler.getText().split("=");
                    if (equation.length > 1) {
                        mHandler.setText(equation[0] + "=(" + equation[1] + ")");
                    } else {
                        mHandler.setText(equation[0] + "=()");
                    }
                } else {
                    mHandler.setText("(" + mHandler.getText() + ")");
                }
                returnToBasic();
                break;

            case R.id.mod:
                if (mHandler.getText().equals(mErrorString)) mHandler.setText("");
                if (mHandler.getText().contains("=")) {
                    String[] equation = mHandler.getText().split("=");
                    if (equation.length > 1) {
                        mHandler.setText(equation[0] + "=" + mModString + "(" + equation[1] + ",");
                    } else {
                        mHandler.insert(mModString + "(");
                    }
                } else {
                    if (mHandler.getText().length() > 0) {
                        mHandler.setText(mModString + "(" + mHandler.getText() + ",");
                    } else {
                        mHandler.insert(mModString + "(");
                    }
                }
                returnToBasic();
                break;

            case R.id.easter:
                Toast.makeText(mContext, R.string.easter_egg, Toast.LENGTH_SHORT).show();
                break;

            case R.id.zoomIn:
                mGraphDisplay.zoomIn();
                break;

            case R.id.zoomOut:
                mGraphDisplay.zoomOut();
                break;

            case R.id.zoomReset:
                mGraphDisplay.zoomReset();
                break;

            default:
                if (view instanceof Button) {
                    String text = ((Button) view).getText().toString();
                    if (text.equals(mDX) || text.equals(mDY)) {
                        // Do nothing
                    } else if (text.length() >= 2) {
                        // Add paren after sin, cos, ln, etc. from buttons
                        text += "(";
                    }
                    mHandler.insert(text);
                    returnToBasic();
                }
        }
    }

    private void vibrate() {
        if (CalculatorSettings.vibrateOnPress(mContext)) {
            Vibrator vi = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            if (!vi.hasVibrator()) return;
            vi.vibrate(CalculatorSettings.getVibrationStrength());
        }
    }

    private boolean returnToBasic() {
        if (mPager != null && CalculatorSettings.returnToBasic(mContext)) {
            Page basic = new Page(mContext, NormalPanel.BASIC);
            if (CalculatorSettings.isPageEnabled(mContext, basic)) {
                ((Calculator) mContext).scrollToPage(basic);
            }
            return true;
        }
        return false;
    }

    private void applyBannedResources(Base base) {
        CalculatorViewPager pager = mPager != null ? mPager : mSmallPager;
        ((CalculatorPageAdapter) pager.getAdapter()).applyBannedResources(base);

        // A special check when mLargePager exists
        if (mLargePager != null) {
            ((CalculatorPageAdapter) mLargePager.getAdapter()).applyBannedResources(base);
        }

        // Update the buttons on the hex page(s)
        Iterable<View> iterator = ((CalculatorPageAdapter) pager.getAdapter()).getViewIterator();
        for (View child : iterator) {
            if (child != null) {
                updateBaseButtons(base, child);
            }
        }
    }

    private void updateBaseButtons(Base baseMode, View parent) {
        int id = 0;
        switch (baseMode) {
            case HEXADECIMAL:
                id = R.id.hex;
                break;
            case BINARY:
                id = R.id.bin;
                break;
            case DECIMAL:
                id = R.id.dec;
                break;
        }
        View v = parent.findViewById(id);
        if (v != null) {
            clearSelectedBase(parent);
            v.setSelected(true);
        }
    }

    private void clearSelectedBase(View parent) {
        View hex = parent.findViewById(R.id.hex);
        View bin = parent.findViewById(R.id.bin);
        View dec = parent.findViewById(R.id.dec);

        hex.setSelected(false);
        bin.setSelected(false);
        dec.setSelected(false);
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.del:
                mHandler.onClear();
                return true;

            case R.id.next:
                // Handle back
                EditText active = mHandler.mDisplay.getActiveEditText();
                if (active.getSelectionStart() == 0) {
                    View v = mHandler.mDisplay.getActiveEditText().focusSearch(View.FOCUS_BACKWARD);
                    if (v != null) v.requestFocus();
                    active = mHandler.mDisplay.getActiveEditText();
                    active.setSelection(active.getText().length());
                } else {
                    active.setSelection(active.getSelectionStart() - 1);
                }
                return true;
        }
        if (view.getTag() != null) {
            String text = (String) view.getTag();
            if (!text.isEmpty()) {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        if (view instanceof TextView && ((TextView) view).getHint() != null) {
            String text = ((TextView) view).getHint().toString();
            if (text.length() >= 2) {
                // Add paren after sin, cos, ln, etc. from buttons
                text += "(";
            }
            mHandler.insert(text);
            returnToBasic();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        int action = keyEvent.getAction();

        // Work-around for spurious key event from IME, bug #1639445
        if (action == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return true; // eat it
        }

        if (keyEvent.getUnicodeChar() == '=') {
            if (action == KeyEvent.ACTION_UP) {
                mHandler.onEnter();
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_ENTER) {
            if (keyEvent.isPrintingKey() && action == KeyEvent.ACTION_UP) {
                // Tell the handler that text was updated.
                mHandler.onTextChanged();
            }
            return false;
        }

        /*
         * We should act on KeyEvent.ACTION_DOWN, but strangely sometimes the DOWN event isn't received, only the UP. So the workaround is to act on UP... http://b/issue?id=1022478
         */

        if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    mHandler.onEnter();
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    mHandler.onUp();
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mHandler.onDown();
                    break;
            }
        }
        return true;
    }

    public void setGraphDisplay(GraphView display) {
        mGraphDisplay = display;
    }
}
