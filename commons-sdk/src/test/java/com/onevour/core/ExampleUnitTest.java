package com.onevour.core;

import android.content.Context;
import android.util.Log;

import com.onevour.core.utilities.beans.BeanCopy;
import com.onevour.core.utilities.beans.SampleBean;
import com.onevour.core.utilities.http.ApiRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class, ApiRequest.class})
public class ExampleUnitTest {

    @Mock
    Context context;

    @Test
    public void test_copy_bean() throws Exception {
        SampleBean source = new SampleBean();
        source.setName("ZULIADIN");
        source.setLastName("NO LAST NAME");
        source.setBod(new Date());
        SampleBean.Education education = new SampleBean.Education();
        education.setEducation("UBL");
        source.setEducation(education);

        SampleBean.Education educationSD = new SampleBean.Education();
        educationSD.setEducation("SD 01");
        List<SampleBean.Education> educationList = new ArrayList<>();
        educationList.add(educationSD);

        source.setEducations(educationList);

        SampleBean target = new SampleBean();
        BeanCopy.copyAllFields(target, source, "name", "date", "lastName");
        System.out.println("Result copy: " + target.getLastName() + " | " + target.getBod() + "|" + target.getEducation().getEducation() + "|" + target.getEducations().size());
        for (SampleBean.Education child : target.getEducations()) {
            System.out.println("education is " + child.getEducation());
        }
    }

    @Test
    public void test_http() throws Exception {
        Log.d("API-TEST", "INFO START");
//        ApiRequestTest request = new ApiRequestTest(context, "https://api.digitalrecordcard.com/index.php/api_v1/login", new HttpListener<String>() {
//            @Override
//            public void onSuccess(String response) {
//                Log.d("API-TEST", response);
//            }
//
//            @Override
//            public void onError(Error httpError) {
//                Log.e("API-TEST", httpError.getMessage());
//            }
//        });
//        request.request();
//        Thread.sleep(15000);
//        assertEquals(4, 2 + 2);
    }
}