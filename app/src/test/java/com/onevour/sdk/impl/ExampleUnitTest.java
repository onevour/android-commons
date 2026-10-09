package com.onevour.sdk.impl;

import org.junit.Test;

import static org.junit.Assert.*;

import com.onevour.core.utilities.beans.BeanCopy;
import com.onevour.sdk.impl.repositories.models.Employee;
import com.onevour.sdk.impl.repositories.models.Person;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        Employee employee = new Employee("1", "John Doe");
        Person person = BeanCopy.value(employee, Person.class);
        assertEquals("John Doe", person.getName());
        assertEquals(0, person.getId());
    }

}