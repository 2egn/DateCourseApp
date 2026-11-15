package com.datecourse.service;

import com.datecourse.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class CoursePreviewImageTest {
    @Test void mapCompositionPreservesTheEntireImageIncludingAttributionFooter() throws Exception {
        var map=new java.awt.image.BufferedImage(1000,520,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=map.createGraphics();
        graphics.setColor(java.awt.Color.BLUE);graphics.fillRect(0,0,1000,520);
        graphics.setColor(java.awt.Color.RED);graphics.fillRect(0,510,1000,10);graphics.dispose();
        var plan=new RoutePlan(35.15,129.05,2,1000,"내 코스",List.of());
        var image=ImageIO.read(new ByteArrayInputStream(new CoursePreviewImage().renderWithMap(plan,map)));
        assertEquals(650,image.getHeight());
        assertEquals(java.awt.Color.BLUE.getRGB(),image.getRGB(0,100));
        assertEquals(java.awt.Color.RED.getRGB(),image.getRGB(999,619));
    }

    @Test void rendersLongNamesAndCoincidentCoordinatesWithoutExternalServices() throws Exception {
        var places = new ArrayList<Place>();
        for(int i=1;i<=30;i++) places.add(new Place(i,"place-"+i,"아주 긴 한글 장소 이름을 가진 식당 "+i,"MEAL",35.15,129.05,10000,4));
        var plan = new RoutePlan(35.15,129.05,2,1000,"내 코스",places);
        byte[] png = new CoursePreviewImage().render(plan);
        assertTrue(png.length<2_000_000);
        var image=ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(1000,image.getWidth());assertEquals(420,image.getHeight());
        var renamed=new RoutePlan(plan.startLat(),plan.startLng(),plan.people(),plan.radius(),"새 이름",plan.places());
        assertEquals(CoursePreviewImage.fingerprint(plan),CoursePreviewImage.fingerprint(renamed));
        var changed=new RoutePlan(plan.startLat(),plan.startLng(),3,plan.radius(),plan.title(),plan.places());
        assertNotEquals(CoursePreviewImage.fingerprint(plan),CoursePreviewImage.fingerprint(changed));
    }
}
