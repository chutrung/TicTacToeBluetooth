package trungchu.com.tictactoebluetooth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Arrays;


import static trungchu.com.tictactoebluetooth.constant.BoardConstant.CELL_SIZE;
import static trungchu.com.tictactoebluetooth.constant.BoardConstant.NUM_COL;
import static trungchu.com.tictactoebluetooth.constant.BoardConstant.NUM_ROW;

/**
 * Board for game
 * O for server, X for client
 */
public class BoardGame extends View {

    private PointF mPivotBoard = new PointF(0, 0);
    private Paint mPaintLine;
    private Paint mPaintMineCell;
    private Paint mPaintEnemyCell;
    private float mBoardWidth;
    private float mBoardHeight;
    private float mCellWidth;
    private float mCellHeight;
    private int mCols;
    private int mRows;
    private float mPaddingCell;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private float mScaleFactor = 1.0f;
    private int[][] board; // value: -1 (empty), 0 (mine), 1(enemy)
    public static boolean isServer; //true : server / false: client
    public static boolean myTurn;
    private SendMessageListener sendMessageListener;

    public BoardGame(Context context) {
        this(context, null, 0);
    }

    public BoardGame(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoardGame(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCols = NUM_COL;
        mRows = NUM_ROW;
        mPaddingCell = 5.f;

        board = new int[mRows][mCols];
        for (int[] row : board) {
            Arrays.fill(row, -1);
        }

        int widthDd = CELL_SIZE * mCols;
        mBoardWidth = convertDpToPixel(widthDd);
        mCellWidth = mBoardWidth / mCols;

        int heightDp = CELL_SIZE * mRows;
        mBoardHeight = convertDpToPixel(heightDp);
        mCellHeight = mBoardHeight / mRows;

        mPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setStrokeWidth(2.f);
        mPaintLine.setColor(Color.BLACK);

        Paint paintGreenCell = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGreenCell.setStyle(Paint.Style.STROKE);
        paintGreenCell.setStrokeWidth(3.f);
        paintGreenCell.setColor(Color.GREEN);

        Paint paintRedCell = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRedCell.setStyle(Paint.Style.STROKE);
        paintRedCell.setStrokeWidth(3.f);
        paintRedCell.setColor(Color.RED);

        mPaintMineCell = paintRedCell;
        mPaintEnemyCell = paintGreenCell;

        //Setup for scale
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        //Scroll
        mGestureDetector = new GestureDetector(getContext(), mGestureListener);
    }

    private float convertDpToPixel(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if ((mPivotBoard.x < 0 && distanceX < 0) || (mPivotBoard.x * mScaleFactor + mBoardWidth * mScaleFactor > getWidth() && distanceX > 0)) {
                mPivotBoard.x -= distanceX;
            }
            if ((mPivotBoard.y < 0 && distanceY < 0) || (mPivotBoard.y * mScaleFactor + mBoardHeight * mScaleFactor > getHeight() && distanceY > 0)) {
                mPivotBoard.y -= distanceY;
            }
            //don't allow scroll over pivot
            if (mPivotBoard.x > 0) mPivotBoard.x = 0;
            if (mPivotBoard.y > 0) mPivotBoard.y = 0;
            //re-draw view port
            invalidate();
            //programer handled this function, so return 'true' to system dont't need handle anymore
            return true;
        }

        /**
         * handle single tab event on map
         * @param e motionEvent when tab on map
         * @return
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            Point point = getCurrentCell(x, y);
            if (myTurn && point != null && board[point.x][point.y] == -1) {
//                myTurn = false;
//                if (isServer)
//                    board[point.x][point.y] = 0;
//                else {
//                    board[point.x][point.y] = 1;
//                }
//                invalidate();
                if (sendMessageListener != null) {
                    sendMessageListener.sendMessage(point);
                }
            }
            return super.onSingleTapUp(e);
        }
    };

    public void setSendMessageListener(SendMessageListener sendMessageListener) {
        this.sendMessageListener = sendMessageListener;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 3.0f));

            // Do not allow scale down if all element is being drawn
            invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            super.onScaleEnd(scaleGestureDetector);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cellWidth = this.mCellWidth * mScaleFactor;
        float boardWidth = this.mBoardWidth * mScaleFactor;

        float cellHeight = this.mCellHeight * mScaleFactor;
        float boardHeight = this.mBoardHeight * mScaleFactor;

        //draw vertical line
        for (int i = 1; i < mCols + 1; i++) {
            canvas.drawLine(i * cellWidth + mPivotBoard.x * mScaleFactor, mPivotBoard.y * mScaleFactor, i * cellWidth + mPivotBoard.x * mScaleFactor, boardHeight + mPivotBoard.y * mScaleFactor, mPaintLine);
        }

        // draw Horizontal line
        for (int i = 1; i < mRows + 1; i++) {
            canvas.drawLine(mPivotBoard.x * mScaleFactor, i * cellHeight + mPivotBoard.y * mScaleFactor, boardWidth + mPivotBoard.x * mScaleFactor, i * cellHeight + mPivotBoard.y * mScaleFactor, mPaintLine);
        }

        // Calculate object cell padding
        mPaddingCell = mPaddingCell * mScaleFactor;

        //draw signal's position
        for (int i = 0; i < mRows; i++) {
            float cx = convertRowIndxToTopScaledPixel(i);
            for (int j = 0; j < mCols; j++) {
                if (board[i][j] == 0) {
                    //ve quan 'O'
                    canvas.drawCircle(cx + cellWidth / 2, convertColIndxToLeftScaledPixel(j) + cellHeight / 2, cellWidth / 3, mPaintMineCell);
                } else if (board[i][j] == 1) {
                    //ve quan 'X'
                    canvas.drawLine(cx, convertColIndxToLeftScaledPixel(j), convertRowIndxToTopScaledPixel(i + 1), convertColIndxToLeftScaledPixel(j + 1), mPaintEnemyCell);
                    canvas.drawLine(convertRowIndxToTopScaledPixel(i + 1), convertColIndxToLeftScaledPixel(j), convertRowIndxToTopScaledPixel(i), convertColIndxToLeftScaledPixel(j + 1), mPaintEnemyCell);
                }
            }
        }
    }

    /**
     * convert row index to top scaled pixel
     *
     * @param row row
     * @return tung do cua row luc scale hien tai
     */
    private float convertRowIndxToTopScaledPixel(int row) {
        return (mPivotBoard.x) * mScaleFactor + row * mCellWidth * mScaleFactor;
    }

    /**
     * convert column index to left scale pixel
     *
     * @param col column
     * @return hoanh do cua col luc scale hien tai
     */
    private float convertColIndxToLeftScaledPixel(int col) {
        return (mPivotBoard.y) * mScaleFactor + col * mCellWidth * mScaleFactor;
    }

    /**
     * get current cell from value of position
     *
     * @param x coordinates x of touch
     * @param y coordinates y of touch
     * @return cell's position
     */
    private Point getCurrentCell(float x, float y) {
        int i = (int) Math.floor((x - (mPivotBoard.x) * mScaleFactor) / (mCellWidth * mScaleFactor));
        int j = (int) Math.floor((y - (mPivotBoard.y) * mScaleFactor) / (mCellWidth * mScaleFactor));
        if (i < NUM_ROW && j < NUM_COL) {
            return new Point(i, j);
        } else
            return null;
    }

    public void setEnemyPointToBoard(Point point) {
        if (isServer)
            board[point.x][point.y] = 1;
        else
            board[point.x][point.y] = 0;

        invalidate();
    }

    public void setMinePointToBoard(Point point) {
        if (isServer)
            board[point.x][point.y] = 1;
        else
            board[point.x][point.y] = 0;

        invalidate();
    }

    public boolean checkWin(Point point) {
        int X = point.x;
        int Y = point.y;
        int value = board[X][Y]; // value ==0 hoac ==1 vi point la cell da duoc danh dau
        //value == 1
        //check column
        //di len cho den khi gap 0 hoac bien
        int up = Y, down = Y;
        while (up - 1 > -1 && board[X][up - 1] == value) {
            --up;
        }
        while (down + 1 < mRows && board[X][down + 1] == value) {
            ++down;
        }
        if (down - up + 1 == 4) { // 4 cell lien nhau cung gia tri
            if ((up - 1 > -1 && board[X][up - 1] == Math.abs(1 - value))
                    || (down + 1 < mRows && board[X][down + 1] == Math.abs(1 - value))) //bi chan 1 trong 2 dau thi false
                return false;
            else
                return true;
        } else if (down - up + 1 == 5) { // 5 cell lien nhau cung gia tri
            return true;
        }
        //check row
        //di sang trai cho den khi gap 0 hoac bien
        int left = X, right = X;
        while (left - 1 > -1 && board[left - 1][Y] == value) {
            --left;
        }
        while (right + 1 < mCols && board[right + 1][Y] == value) {
            ++right;
        }
        if (right - left + 1 == 4) {
            if ((left > 0 && board[left - 1][Y] == Math.abs(1 - value))
                    || (right < mCols - 1 && board[right + 1][Y] == Math.abs(1 - value)))
                return false; //bi chan 1 trong 2 dau
            else return true;
        } else if (right - left + 1 == 5)
            return true;
        //check diagonal
        //cheo tu trai (\)
        int x_left = X, y_left = Y, x_right = X, y_right = Y;
        while (x_left - 1 > -1 && y_left - 1 > -1 && board[x_left - 1][y_left - 1] == value) {
            --x_left;
            --y_left;
        }
        while (x_right + 1 < mCols && y_right + 1 < mRows && board[x_right + 1][y_right + 1] == value) {
            ++x_right;
            ++y_right;
        }
        if (x_right - x_left + 1 == 4) {
            if ((x_left - 1 > -1 && y_left - 1 > -1 && board[x_left - 1][y_left - 1] == Math.abs(1 - value)) ||
                    (x_right + 1 < mCols && y_right + 1 < mRows && board[x_right + 1][y_right + 1] == Math.abs(1 - value))) {
                return false;
            } else return true;
        } else if (x_right - x_left + 1 == 5) {
            return true;
        }
        //cheo tu phai (/)
        x_left = X;
        y_left = Y;
        x_right = X;
        y_right = Y;
        while (x_left - 1 > -1 && y_left + 1 < mRows && board[x_left - 1][y_left + 1] == value) {
            --x_left;
            ++y_left;
        }
        while (x_right + 1 < mCols && y_right - 1 > -1 && board[x_right + 1][y_right - 1] == value) {
            ++x_right;
            --y_right;
        }
        if (x_right - x_left + 1 == 4) {
            if ((x_left - 1 > -1 && y_left + 1 < mRows && board[x_left - 1][y_left + 1] == Math.abs(1 - value)) ||
                    (x_right + 1 < mCols && y_right - 1 > -1 && board[x_right + 1][y_right - 1] == Math.abs(1 - value))) {
                return false;
            } else return true;
        } else if (x_right - x_left + 1 == 5) {
            return true;
        }
        return false;
    }

    public void clearMap(){
        for (int[] row : board) {
            Arrays.fill(row, -1);
        }
        invalidate();
    }
}
