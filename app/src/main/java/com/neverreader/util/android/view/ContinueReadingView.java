package com.neverreader.util.android.view;

import android.content.Context;
import android.view.View;

import androidx.cardview.widget.CardView;

import com.ideashower.readitlater.R;
import com.neverreader.app.list.ContinueReading;
import com.neverreader.sdk.api.generated.enums.CxtUi;
import com.neverreader.sdk.api.generated.enums.CxtView;
import com.neverreader.sdk.api.generated.thing.ActionContext;
import com.neverreader.sdk.api.generated.thing.Item;
import com.neverreader.sdk2.analytics.context.Contextual;
import com.neverreader.sdk2.analytics.context.Interaction;
import com.neverreader.sdk2.view.ModelBindingHelper;
import com.neverreader.ui.view.notification.ItemSnackbarView;
import com.neverreader.ui.view.notification.PktSwipeDismissBehavior;
import com.neverreader.util.android.ViewUtil;

public class ContinueReadingView extends ItemSnackbarView implements Contextual {

    private ContinueReadingView(Context context) {
        super(context);
        final int padding = getResources().getDimensionPixelSize(com.neverreader.ui.R.dimen.pkt_space_sm) - (int) ((CardView) findViewById(
				com.neverreader.ui.R.id.card)).getCardElevation();
        setPadding(padding, 0, padding, padding);
    }

    @Override
    public ActionContext getActionContext() {
        return new ActionContext.Builder()
                .cxt_view(CxtView.LIST)
                .cxt_ui(CxtUi.CONTINUE_READING).build();
    }

    public static ContinueReadingView getInstance(
            Context context,
            Item item,
            ContinueReading continueReading,
            OnClickListener listener
    ) {
        ContinueReadingView continueReadingView = new ContinueReadingView(context);
        var modelBindingHelper = new ModelBindingHelper(context);
        continueReadingView.bind().clear()
                .onClick(listener)
                .icon(com.neverreader.ui.R.drawable.ic_pkt_reading_line_mini)
                .featureTitle(context.getResources().getText(R.string.lb_continue_reading))
                .onDismiss(new PktSwipeDismissBehavior.OnDismissListener() {
                    @Override
                    public void onDismiss(View v) {
                        ViewUtil.remove(continueReadingView);
                        continueReading.trackDismiss(Interaction.on(continueReadingView));
                    }

                    @Override
                    public void onDragStateChanged(int state) {
                        //
                    }
                })
                .meta()
                .title(modelBindingHelper.title(item, null, false, false, "").value)
                .domain(modelBindingHelper.domain(item, null, false, false, "").value)
                .timeEstimate(modelBindingHelper.timeLeftEstimate(item));

        return continueReadingView;
    }
}
