FROM flink:1.13.2-scala_2.12-java8

ADD ./dependencies-job/build/libs/dependencies-job-all.jar /opt/flink/usrlib/dependencies-job.jar
ADD ./common/build/libs/common.jar /opt/flink/usrlib/common.jar