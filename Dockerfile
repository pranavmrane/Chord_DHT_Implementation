FROM openjdk:8

# Copy Content
COPY src/ /tmp/

RUN mkdir -p /Chord

RUN mv /tmp/* /Chord