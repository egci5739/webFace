package com.face.nd.entity;

import com.sun.jna.Structure;
import org.springframework.stereotype.Component;

@Component
public class FaceInfoEntity extends Structure {

    public byte[] byFaceInfo;

}