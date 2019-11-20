package no.ssb.data.catalog.server;

import io.grpc.stub.StreamObserver;
import no.ssb.data.catalog.api.*;

import javax.inject.Singleton;

@Singleton
public class DatasetCatalogService extends DatasetCatalogServiceGrpc.DatasetCatalogServiceImplBase {

    @Override
    public void list(ListDatasetRequest request, StreamObserver<ListDatasetResponse> responseObserver) {

        // foo.bar ->
        ScalarType.Builder stringType = ScalarType.newBuilder().setString(StringType.newBuilder().build());
        ScalarType.Builder longType = ScalarType.newBuilder().setNumeric(NumericType.newBuilder()
                .setLong(LongType.newBuilder())
                .build());

        Dataset dataset = Dataset.newBuilder().setStructure(
                StructType.newBuilder()
                        .addTypes(StructField.newBuilder().setName("PERSON_ID")
                                .setType(DataType.newBuilder().setScalar(stringType)))
                        .addTypes(StructField.newBuilder().setName("INCOME")
                                .setType(DataType.newBuilder().setScalar(longType)))
                        .addTypes(StructField.newBuilder().setName("GENDER").setType(DataType.newBuilder()
                                .setScalar(stringType)))
                        .addTypes(StructField.newBuilder().setName("MARITAL_STATUS").setType(DataType.newBuilder()
                                .setScalar(stringType)))
                        .addTypes(StructField.newBuilder().setName("MUNICIPALITY").setType(DataType.newBuilder()
                                .setScalar(stringType)))
                        .addTypes(StructField.newBuilder().setName("DATA_QUALITY").setType(DataType.newBuilder()
                                .setScalar(stringType)))
                        .build()
        ).build();

        ListDatasetResponse response = ListDatasetResponse.newBuilder()
                .setNs(request.getNs())
                .addDatasets(dataset)
                .setCount(1)
                .setOffset(0)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDataset(DatasetIdentifier request, StreamObserver<Dataset> responseObserver) {
        super.getDataset(request, responseObserver);
    }

    @Override
    public void saveDataset(Dataset request, StreamObserver<Dataset> responseObserver) {
        super.saveDataset(request, responseObserver);
    }

    @Override
    public void updateDataset(Dataset request, StreamObserver<Dataset> responseObserver) {
        super.updateDataset(request, responseObserver);
    }
}
