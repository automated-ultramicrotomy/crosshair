/*
 * CustomXmlIoSpimData is adapted from the imagej-utils repository -
 * https://github.com/embl-cba/imagej-utils - released under a BSD 2-Clause license given below:
 *
 * Copyright (c) 2018 - 2024, EMBL
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package de.embl.schwab.crosshair.utils;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.XmlIoSequenceDescription;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.InputStream;

import static mpicbg.spim.data.XmlKeys.SPIMDATA_TAG;


public class CustomXmlIoSpimData extends XmlIoAbstractSpimData< SequenceDescription, SpimData> {
    public CustomXmlIoSpimData() {
        super(SpimData.class, new XmlIoSequenceDescription(), new XmlIoViewRegistrations());
    }

    /**
     * Load 'SpimData' from an input stream. NOTE we still need to pass the xml filename here, so that bdv can
     * determine the relative paths for local files.
     * @param in input stream
     * @param xmlFilename xml filename
     * @return SpimData instance
     */
    public SpimData loadFromStream(InputStream in, String xmlFilename) throws SpimDataException {
        final SAXBuilder sax = new SAXBuilder();
        Document doc;
        try
        {
            doc = sax.build( in );
        }
        catch ( final Exception e )
        {
            throw new SpimDataIOException( e );
        }
        final Element root = doc.getRootElement();

        if ( root.getName() != SPIMDATA_TAG )
            throw new RuntimeException( "expected <" + SPIMDATA_TAG + "> root element. wrong file?" );

        return fromXml( root, new File( xmlFilename ) );
    }
}
