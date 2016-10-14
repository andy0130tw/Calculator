package com.android2.calculator3;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android2.calculator3.view.BackspaceImageButton;
import com.android2.calculator3.view.FormattedNumberEditText;
import com.xlythe.math.Constants;
import com.xlythe.math.EquationFormatter;
import com.xlythe.math.History;
import com.xlythe.math.HistoryEntry;
import com.xlythe.math.Persist;
import com.xlythe.math.Solver;

public class MainActivity extends WearableActivity {

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    // Calc logic
    private View.OnClickListener mListener;
    private ViewSwitcher mDisplay;
    private BackspaceImageButton mDelete;
    private ViewPager mPager;
    private Persist mPersist;
    private History mHistory;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;
    private State mState;

    private enum State {
        DELETE, CLEAR, ERROR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // Rebuild constants. If the user changed their locale, it won't kill the app
        // but it might change a decimal point from . to ,
        Constants.rebuildConstants();

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        mPager = (ViewPager) findViewById(R.id.panelswitch);

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.getHistory();

        mDisplay = (ViewSwitcher) findViewById(R.id.display);
        for (FormattedNumberEditText editText : getEditTexts()) {
            editText.setSolver(mEvaluator.getSolver());
        }

        mDelete = (BackspaceImageButton) findViewById(R.id.delete);
        mListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.delete:
                        if (mDelete.getState() == BackspaceImageButton.State.CLEAR) {
                            mDisplay.showNext();
                            onClear();
                        } else {
                            onDelete();
                        }
                        break;
                    case R.id.eq:
                        mEvaluator.evaluate(getActiveEditText().getCleanText(), new CalculatorExpressionEvaluator.EvaluateCallback() {
                            @Override
                            public void onEvaluate(String expr, String result, int errorResourceId) {
                                mDisplay.showNext();
                                if (errorResourceId != MainActivity.INVALID_RES_ID) {
                                    onError(errorResourceId);
                                } else {
                                    setText(result);
                                }
                                if (saveHistory(expr, result)) {
                                    RecyclerView history = (RecyclerView) findViewById(R.id.history);
                                    history.getLayoutManager().scrollToPosition(history.getAdapter().getItemCount() - 1);
                                }
                            }
                        });
                        break;
                    case R.id.parentheses:
                        setText("(" + getActiveEditText().getText() + ")");
                        break;
                    default:
                        if(((Button) v).getText().toString().length() >= 2) {
                            onInsert(((Button) v).getText().toString() + "(");
                        } else {
                            onInsert(((Button) v).getText().toString());
                        }
                        break;
                }
            }
        };
        mDelete.setOnClickListener(mListener);
        mDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mDelete.getState() == BackspaceImageButton.State.DELETE) {
                    mDisplay.showNext();
                    onClear();
                    return true;
                }

                return false;
            }
        });


        HistoryAdapter.HistoryItemCallback historyItemCallback = new HistoryAdapter.HistoryItemCallback() {
            @Override
            public void onHistoryItemSelected(HistoryEntry entry) {
                setState(State.DELETE);
                getActiveEditText().insert(entry.getResult());
            }
        };
        final CalculatorPageAdapter adapter = new CalculatorPageAdapter(
                this, mListener, historyItemCallback, mEvaluator.getSolver(), mHistory);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(1);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int mActivePage = -1;

            @Override
            public void onPageScrolled(int i, float v, int i1) {
                // We're scrolling, so enable everything
                if (mActivePage != -1) {
                    mActivePage = -1;
                    setActivePage(mActivePage);
                }
            }

            @Override
            public void onPageSelected(int i) {
                // We've landed on a page, so disable all pages but this one
                mActivePage = i;
                setActivePage(mActivePage);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // We've landed on a page (possibly the current page) so disable all pages but this one
                if (mActivePage == -1) {
                    mActivePage = mPager.getCurrentItem();
                    setActivePage(mActivePage);
                }
            }

            private void setActivePage(int page) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    adapter.setEnabled(adapter.getViewAt(i), page == -1 || i == page);
                }
            }
        });

        setState(State.DELETE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPersist.save();
    }

    private void onDelete() {
        setState(State.DELETE);
        getActiveEditText().backspace();
    }

    private void onClear() {
        setState(State.DELETE);
        getActiveEditText().clear();
    }

    private void setText(String text) {
        setState(State.CLEAR);
        getActiveEditText().setText(text);
    }

    private void onInsert(String text) {
        if (mState == State.ERROR || (mState == State.CLEAR && !Solver.isOperator(text))) {
            setText(text);
        } else {
            getActiveEditText().insert(text);
        }

        setState(State.DELETE);
    }

    private void onError(int resId) {
        setState(State.ERROR);
        getActiveEditText().setText(resId);
    }

    private void setState(State state) {
        mDelete.setState(state == State.DELETE ? BackspaceImageButton.State.DELETE : BackspaceImageButton.State.CLEAR);
        if(mState != state) {
            switch (state) {
                case CLEAR:
                    getActiveEditText().setTextColor(getResources().getColor(R.color.display_formula_text_color));
                    break;
                case DELETE:
                    getActiveEditText().setTextColor(getResources().getColor(R.color.display_formula_text_color));
                    break;
                case ERROR:
                    getActiveEditText().setTextColor(getResources().getColor(R.color.calculator_error_color));
                    break;
            }
            mState = state;
        }
    }

    private FormattedNumberEditText getActiveEditText() {
        return (FormattedNumberEditText) mDisplay.getCurrentView();
    }

    protected boolean saveHistory(String expr, String result) {
        if (mHistory == null) {
            return false;
        }

        if (!TextUtils.isEmpty(expr)
                && !TextUtils.isEmpty(result)
                && !Solver.equal(expr, result)
                && (mHistory.current() == null || !mHistory.current().getFormula().equals(expr))) {
            expr = EquationFormatter.appendParenthesis(expr);
            expr = Solver.clean(expr);
            expr = mTokenizer.getLocalizedExpression(expr);
            mHistory.enter(expr, result);
            return true;
        }
        return false;
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        setEnabled(findViewById(android.R.id.content), false);
        Drawable background = findViewById(android.R.id.content).getRootView().getBackground();
        findViewById(R.id.display_background).setBackground(background);
        findViewById(R.id.pad_advanced).setBackground(background);
        findViewById(R.id.pad_numeric).setBackground(background);
        findViewById(R.id.pad_operator).setBackground(background);
        findViewById(R.id.pad_history).setBackground(background);
        for (FormattedNumberEditText editText : getEditTexts()) {
            editText.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        setEnabled(findViewById(android.R.id.content), true);
        findViewById(R.id.display_background).setBackgroundResource(R.drawable.white_card);
        findViewById(R.id.pad_advanced).setBackgroundResource(R.color.pad_advanced_background_color);
        findViewById(R.id.pad_numeric).setBackgroundResource(R.color.pad_numeric_background_color);
        findViewById(R.id.pad_operator).setBackgroundResource(R.color.pad_operator_background_color);
        findViewById(R.id.pad_history).setBackgroundResource(R.color.pad_history_background_color);
        for (FormattedNumberEditText editText : getEditTexts()) {
            editText.setTextColor(getResources().getColor(R.color.display_formula_text_color));
        }
    }

    private void setEnabled(View root, boolean enabled) {
        if (root instanceof TextView
                || root instanceof ImageButton
                || root instanceof ViewPager
                || root instanceof RecyclerView) {
            root.setEnabled(enabled);
        }
        if (root instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) root;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }

    private FormattedNumberEditText[] getEditTexts() {
        FormattedNumberEditText[] editTexts = new FormattedNumberEditText[mDisplay.getChildCount()];
        for (int i = 0; i < mDisplay.getChildCount(); i++) {
            editTexts[i] = (FormattedNumberEditText) mDisplay.getChildAt(i);
        }
        return editTexts;
    }
}
