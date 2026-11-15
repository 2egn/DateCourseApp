package com.datecourse.service;

import com.datecourse.model.RoutePlan;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Versioned PNG summary. The editable title is displayed by the surrounding UI. */
public final class CoursePreviewImage implements CoursePreviewRenderer {
    public static final int WIDTH = 1000, HEIGHT = 420;

    /** Only content rendered in the image contributes to its revision. */
    public static String fingerprint(RoutePlan plan) {
        StringBuilder value = new StringBuilder("summary-map-v3\n");
        value.append(plan.startLat()).append(',').append(plan.startLng()).append('\n')
                .append(plan.people()).append('\n').append(plan.costEstimate().display());
        for (var place : plan.places()) value.append('\n').append(place.name().length()).append(':')
                .append(place.name()).append('|').append(place.lat()).append('|').append(place.lng());
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    /** Compose the current SDK map without cropping its logo or copyright footer. */
    public byte[] renderWithMap(RoutePlan plan, BufferedImage map) throws IOException {
        var image=new BufferedImage(1000,650,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(247,249,252));g.fillRect(0,0,1000,650);
            text(g,"코스 미리보기",24,37,25,true,new Color(28,43,65),950);
            text(g,plan.places().size()+"곳 · "+plan.people()+"명 · "+plan.costEstimate().display(),24,74,23,false,new Color(85,101,124),950);
            g.drawImage(map,0,100,1000,520,null);
            text(g,"지도: Kakao · 직선 연결 · 실제 도보 경로와 다를 수 있어요",24,643,17,false,new Color(85,101,124),950);
        } finally {g.dispose();}
        try(var bytes=new ByteArrayOutputStream()) {
            if(!ImageIO.write(image,"png",bytes))throw new IOException("PNG 인코더가 없습니다.");
            return bytes.toByteArray();
        }
    }

    @Override public byte[] render(RoutePlan plan) throws IOException {
        var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(247,249,252)); g.fillRect(0,0,WIDTH,HEIGHT);
            text(g,"코스 미리보기",32,48,26,true,new Color(28,43,65),930);
            text(g,plan.places().size()+"곳 · "+plan.people()+"명 · "+plan.costEstimate().display(),32,83,25,false,new Color(85,101,124),930);
            g.setColor(Color.WHITE); g.fillRoundRect(26,110,560,270,24,24);
            g.fillRoundRect(604,110,370,270,24,24);
            drawConnections(g, plan);
            text(g,"방문 순서",626,145,24,true,new Color(28,43,65),325);
            text(g,"출발지",626,177,20,false,new Color(85,101,124),325);
            int shown = Math.min(6,plan.places().size());
            for(int i=0;i<shown;i++) {
                dot(g,639,198+i*27,Integer.toString(i+1),new Color(239,87,113));
                text(g,plan.places().get(i).name(),662,205+i*27,22,false,new Color(28,43,65),287);
            }
            if(plan.places().size()>shown) text(g,"외 "+(plan.places().size()-shown)+"곳 · 전체 순서는 아래 목록에서 확인",626,367,18,false,new Color(85,101,124),325);
            text(g,"장소 연결도 · 실제 도보 경로와 다를 수 있어요",32,406,20,false,new Color(85,101,124),930);
        } finally { g.dispose(); }
        try(var output = new ByteArrayOutputStream()) {
            if(!ImageIO.write(image,"png",output)) throw new IOException("PNG 인코더를 찾지 못했습니다.");
            return output.toByteArray();
        }
    }

    private void drawConnections(Graphics2D g, RoutePlan plan) {
        int count=plan.places().size()+1;
        double[] xs=new double[count],ys=new double[count];
        double scale=Math.cos(Math.toRadians(plan.startLat()));
        xs[0]=plan.startLng()*scale; ys[0]=-plan.startLat();
        for(int i=1;i<count;i++){var p=plan.places().get(i-1);xs[i]=p.lng()*scale;ys[i]=-p.lat();}
        double minX=java.util.Arrays.stream(xs).min().orElse(0),maxX=java.util.Arrays.stream(xs).max().orElse(0);
        double minY=java.util.Arrays.stream(ys).min().orElse(0),maxY=java.util.Arrays.stream(ys).max().orElse(0);
        double span=Math.max(Math.max(maxX-minX,maxY-minY),0.0005);
        for(int i=0;i<count;i++){xs[i]=306+(xs[i]-(minX+maxX)/2)*210/span;ys[i]=252+(ys[i]-(minY+maxY)/2)*210/span;}
        g.setStroke(new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(new Color(239,142,158));
        for(int i=1;i<count;i++)g.draw(new Line2D.Double(xs[i-1],ys[i-1],xs[i],ys[i]));
        // Draw the start last so coincident stops do not hide the origin.
        for(int i=1;i<count;i++)dot(g,(int)xs[i],(int)ys[i],Integer.toString(i),new Color(239,87,113));
        dot(g,(int)xs[0],(int)ys[0],"S",new Color(51,114,188));
    }

    private void dot(Graphics2D g,int x,int y,String label,Color color){
        g.setColor(Color.WHITE);g.fillOval(x-16,y-16,32,32);
        g.setColor(color);g.fillOval(x-13,y-13,26,26);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,label.length()>2?10:14));
        g.setColor(Color.WHITE);g.drawString(label,x-g.getFontMetrics().stringWidth(label)/2,y+5);
    }
    private void text(Graphics2D g,String value,int x,int y,int size,boolean bold,Color color,int maxWidth){
        g.setFont(new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size));g.setColor(color);
        String shown=value;
        while(shown.length()>0&&g.getFontMetrics().stringWidth(shown)>maxWidth)
            shown=shown.substring(0,shown.length()-1);
        if(!shown.equals(value)&&shown.length()>1)shown=shown.substring(0,shown.length()-1)+"…";
        g.drawString(shown,x,y);
    }
}
