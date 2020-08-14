/*
 * Copyright © 2018 The GWT Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gwtproject.i18n.client;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import org.junit.Before;
import org.junit.Test;

public class ConstantsTest_en {

  @Before
  public void setUp() throws Exception {
    System.setProperty("locale", "en");
  }

  @Test
  public void testConstantBooleans() {
    TestConstants types = TestConstants_Factory.create();
    assertEquals(false, types.booleanFalse());
    assertEquals(true, types.booleanTrue());
  }

  @Test
  public void testConstantDoubles() {
    TestConstants types = TestConstants_Factory.create();
    double delta = 0.0000001;
    assertEquals(3.14159, types.doublePi(), delta);
    assertEquals(0.0, types.doubleZero(), delta);
    assertEquals(1.0, types.doubleOne(), delta);
    assertEquals(-1.0, types.doubleNegOne(), delta);
    assertEquals(Double.MAX_VALUE, types.doublePosMax(), delta);
    assertEquals(Double.MIN_VALUE, types.doublePosMin(), delta);
    assertEquals(-Double.MAX_VALUE, types.doubleNegMax(), delta);
    assertEquals(-Double.MIN_VALUE, types.doubleNegMin(), delta);
  }

  @Test
  public void testConstantFloats() {
    TestConstants types = TestConstants_Factory.create();
    double delta = 0.0000001;
    assertEquals(3.14159f, types.floatPi(), delta);
    assertEquals(0.0f, types.floatZero(), delta);
    assertEquals(1.0f, types.floatOne(), delta);
    assertEquals(-1.0f, types.floatNegOne(), delta);
    assertEquals(Float.MAX_VALUE, types.floatPosMax(), delta);
    assertEquals(Float.MIN_VALUE, types.floatPosMin(), delta);
    assertEquals(-Float.MAX_VALUE, types.floatNegMax(), delta);
    assertEquals(-Float.MIN_VALUE, types.floatNegMin(), delta);
  }

  /** Exercises ConstantMap more than the other map tests. */
  @Test
  public void testConstantMapABCD() {
    TestConstants types = TestConstants_Factory.create();

    Map<String, String> map = types.mapABCD();
    Map<String, String> expectedMap =
        getMapFromArrayUsingASimpleRule(new String[] {"A", "B", "C", "D"});
    assertNull(map.get("bogus"));
    compareMapsComprehensively(map, expectedMap);

    /*
     * Test if the returned map can be modified in any way. Things are working
     * as expected if exceptions are thrown in each case.
     */
    String failureMessage = "Should have thrown UnsupportedOperationException";
    /* test map operations */
    try {
      map.remove("keyA");
      fail(failureMessage + " on map.remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.put("keyA", "allA");
      fail(failureMessage + "on map.put of existing key");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.put("keyZ", "allZ");
      fail(failureMessage + "on map.put of new key");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.clear();
      fail(failureMessage + " on map.clear");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.keySet() operations */
    try {
      map.keySet().add("keyZ");
      fail(failureMessage + " on map.keySet().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.keySet().remove("keyA");
      fail(failureMessage + " on map.keySet().remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.keySet().clear();
      fail(failureMessage + " on map.keySet().clear");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.values() operations */
    try {
      map.values().add("valueZ");
      fail(failureMessage + " on map.values().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.values().remove("valueA");
      fail(failureMessage + " on map.values().clear()");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.values().clear();
      fail(failureMessage + " on map.values().clear()");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.entrySet() operations */
    Map.Entry<String, String> firstEntry = map.entrySet().iterator().next();
    try {
      map.entrySet().clear();
      fail(failureMessage + "on map.entrySet().clear");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.entrySet().remove(firstEntry);
      fail(failureMessage + " on map.entrySet().remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.entrySet().add(firstEntry);
      fail(failureMessage + "on map.entrySet().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      firstEntry.setValue("allZ");
      fail(failureMessage + "on firstEntry.setValue");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.clear();
      fail(failureMessage + " on map.clear");
    } catch (UnsupportedOperationException e) {
    }
  }

  /** Tests exercise the cache. */
  @Test
  public void testConstantMapBACD() {
    TestConstants types = TestConstants_Factory.create();
    Map<String, String> map = types.mapBACD();
    Map<String, String> expectedMap =
        getMapFromArrayUsingASimpleRule(new String[] {"B", "A", "C", "D"});
    compareMapsComprehensively(map, expectedMap);
  }

  /** Tests exercise the cache. */
  @Test
  public void testConstantMapBBB() {
    TestConstants types = TestConstants_Factory.create();
    Map<String, String> map = types.mapBBB();
    Map<String, String> expectedMap = getMapFromArrayUsingASimpleRule(new String[] {"B"});
    compareMapsComprehensively(map, expectedMap);
  }

  /** Tests exercise the cache and check if Map works as the declared return type. */
  @SuppressWarnings("unchecked")
  @Test
  public void testConstantMapDCBA() {
    TestConstants types = TestConstants_Factory.create();
    Map<String, String> map = types.mapDCBA();
    Map<String, String> expectedMap =
        getMapFromArrayUsingASimpleRule(new String[] {"D", "C", "B", "A"});
    compareMapsComprehensively(map, expectedMap);
  }

  /** Tests focus on correctness of entries, since ABCD exercises the map. */
  @Test
  public void testConstantMapEmpty() {
    TestConstants types = TestConstants_Factory.create();
    Map<String, String> map = types.mapEmpty();
    Map<String, String> expectedMap = new HashMap<String, String>();
    compareMapsComprehensively(map, expectedMap);
  }

  /** Tests exercise the cache and check if Map works as the declared return type. */
  @Test
  public void testConstantMapXYZ() {
    TestConstants types = TestConstants_Factory.create();
    Map<String, String> map = types.mapXYZ();
    Map<String, String> expectedMap = new HashMap<String, String>();
    expectedMap.put("keyX", "valueZ");
    expectedMap.put("keyY", "valueZ");
    expectedMap.put("keyZ", "valueZ");
    compareMapsComprehensively(map, expectedMap);
  }

  @Test
  public void testConstantStringArrays() {
    TestConstants types = TestConstants_Factory.create();
    String[] s;

    s = types.stringArrayABCDEFG();
    assertArrayEquals(new String[] {"A", "B", "C", "D", "E", "F", "G"}, s);

    s = types.stringArraySizeOneEmptyString();
    assertArrayEquals(new String[] {""}, s);

    s = types.stringArraySizeOneX();
    assertArrayEquals(new String[] {"X"}, s);

    s = types.stringArraySizeTwoBothEmpty();
    assertArrayEquals(new String[] {"", ""}, s);

    s = types.stringArraySizeThreeAllEmpty();
    assertArrayEquals(new String[] {"", "", ""}, s);

    s = types.stringArraySizeTwoWithEscapedComma();
    assertArrayEquals(new String[] {"X", ", Y"}, s);

    s = types.stringArraySizeOneWithBackslashX();
    assertArrayEquals(new String[] {"\\X"}, s);

    s = types.stringArraySizeThreeWithDoubleBackslash();
    assertArrayEquals(new String[] {"X", "\\", "Y"}, s);
  }

  @Test
  public void testConstantStrings() {
    TestConstants types = TestConstants_Factory.create();
    assertEquals("string", types.getString());
    assertEquals("stringTrimsLeadingWhitespace", types.stringTrimsLeadingWhitespace());
    assertEquals(
        "stringDoesNotTrimTrailingThreeSpaces   ", types.stringDoesNotTrimTrailingThreeSpaces());
    assertEquals("", types.stringEmpty());
    String jaBlue = types.stringJapaneseBlue();
    assertEquals("あお", jaBlue);
    String jaGreen = types.stringJapaneseGreen();
    assertEquals("みどり", jaGreen);
    String jaRed = types.stringJapaneseRed();
    assertEquals("あか", jaRed);
  }

  @Test
  public void testConstantsWithLookup() {
    TestConstantsWithLookup l = TestConstantsWithLookup_Factory.create();
    Map<String, String> map = l.getMap("mapABCD");
    assertEquals("valueA", map.get("keyA"));
    map = l.getMap("mapDCBA");
    assertEquals("valueD", map.get("keyD"));
    assertEquals(l.mapABCD(), l.getMap("mapABCD"));
    assertEquals(l.mapDCBA(), l.getMap("mapDCBA"));
    assertEquals(l.mapBACD(), l.getMap("mapBACD"));
    assertEquals(l.getString(), l.getString("getString"));
    assertSame(l.stringArrayABCDEFG(), l.getStringArray("stringArrayABCDEFG"));
    assertEquals(l.booleanFalse(), l.getBoolean("booleanFalse"));
    assertEquals(l.floatPi(), l.getFloat("floatPi"), .001);
    assertEquals(l.doublePi(), l.getDouble("doublePi"), .001);
    try {
      // even though getString has the gwt.key "string", it is not the lookup
      // value
      l.getMap("string");
      fail("Should have thrown MissingResourceException");
    } catch (MissingResourceException e) {
      // success if the exception was caught
    }
  }

  // compare the map, entrySet, keySet, and values
  private Map<String, String> getMapFromArrayUsingASimpleRule(String array[]) {
    Map<String, String> map = new HashMap<String, String>();
    for (String str : array) {
      map.put("key" + str, "value" + str);
    }
    return map;
  }

  private void compareMapsComprehensively(
      Map<String, String> map, Map<String, String> expectedMap) {
    // checking both directions to verify that the equals implementation is
    // correct both ways
    assertEquals(expectedMap, map);
    assertEquals(map, expectedMap);
    assertEquals(expectedMap.entrySet(), map.entrySet());
    assertEquals(map.entrySet(), expectedMap.entrySet());
    assertEquals(expectedMap.keySet(), map.keySet());
    assertEquals(map.keySet(), expectedMap.keySet());
    assertTrue(compare(expectedMap.values(), map.values()));
    assertTrue(compare(map.values(), expectedMap.values()));
  }

  private <T> boolean compare(Collection<T> collection1, Collection<T> collection2) {
    if (collection1 == null) {
      return (collection2 == null);
    }
    if (collection2 == null) {
      return false;
    }
    if (collection1.size() != collection2.size()) {
      return false;
    }
    for (T element1 : collection1) {
      boolean found = false;
      for (T element2 : collection2) {
        if (element1.equals(element2)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }
}
