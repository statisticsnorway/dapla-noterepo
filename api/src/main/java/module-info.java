// TODO: rename to protobuf
module dapla.notes.api {
    exports no.ssb.dapla.note.api;

    requires com.google.common;
    requires io.grpc;
    requires grpc.stub;
    requires java.annotation;
    requires protobuf.java;
    requires grpc.protobuf;
}