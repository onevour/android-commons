package org.cise.sdk.ciseapp.modules.main.controllers;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.cise.core.utilities.commons.ContextHelper;
import org.cise.core.utilities.commons.RefSession;
import org.cise.core.utilities.helper.UIHelper;
import org.cise.sdk.ciseapp.R;
import org.cise.sdk.ciseapp.modules.form.controllers.FormSimpleActivity;
import org.cise.sdk.ciseapp.modules.formscroll.controllers.FormScrollActivity;
import org.cise.sdk.ciseapp.modules.fragmentbottom.controllers.FragmentBottomActivity;
import org.cise.sdk.ciseapp.modules.fragment.controllers.FragmentActivity;
import org.cise.sdk.ciseapp.modules.fragmentbottomnavigation.controllers.FragmentBottomNavigationActivity;
import org.cise.sdk.ciseapp.modules.main.components.SampleAdapter;
import org.cise.sdk.ciseapp.modules.main.components.SampleHolder;
import org.cise.sdk.ciseapp.modules.main.models.Sample;
import org.cise.sdk.ciseapp.modules.adapter.controllers.AdapterSampleActivity;
import org.cise.sdk.ciseapp.modules.main.models.SampleMV;
import org.cise.sdk.ciseapp.modules.mvvm.views.MVVMActivity;
import org.cise.sdk.ciseapp.modules.rxjava.controllers.RXActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SampleHolder.Listener {

    private String TAG = "MA-APP";

    @BindView(R.id.rv_sample)
    RecyclerView rvSample;

    RefSession refSession = new RefSession();

    private SampleAdapter adapter = new SampleAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextHelper.init(getApplication());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        UIHelper.initRecyclerView(rvSample, adapter);
        adapter.setHolderListener(this);
        List<SampleMV> samples = new ArrayList<>();
        samples.add(new SampleMV("Adapter", AdapterSampleActivity.class));
        samples.add(new SampleMV("Fragment BackStack", FragmentActivity.class));
        samples.add(new SampleMV("Fragment Bottom", FragmentBottomActivity.class));
        samples.add(new SampleMV("Fragment Bottom Navigation", FragmentBottomNavigationActivity.class));
        samples.add(new SampleMV("Form Simple", FormSimpleActivity.class));
        samples.add(new SampleMV("Form Scroll", FormScrollActivity.class));
        samples.add(new SampleMV("RXJava", RXActivity.class));
        samples.add(new SampleMV("MVVM-Binding", MVVMActivity.class));
        refSession.saveCollection("menu", samples);
        init();
    }

    private void init() {
        List<SampleMV> samples = refSession.findCollection("menu", SampleMV.class);
        adapter.setValue(samples);
    }

    @Override
    public void onSelectedHolder(int index, Sample o) {
        startActivity(new Intent(this, o.getClazz()));
    }


}
