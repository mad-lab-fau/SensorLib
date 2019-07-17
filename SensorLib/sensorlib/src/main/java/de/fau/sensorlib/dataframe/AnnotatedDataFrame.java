/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Data frame for annotations.
 */
public interface AnnotatedDataFrame {

    String[] columns = new String[]{"ann_char", "ann_string"};

    /**
     * Returns the annotation label.
     *
     * @return Annotation label as char
     */
    default char getAnnotationChar() {
        return Character.MIN_VALUE;
    }

    /**
     * Returns the annotation String
     *
     * @return Annotation String
     */
    default String getAnnotationString() {
        return "";
    }
}
