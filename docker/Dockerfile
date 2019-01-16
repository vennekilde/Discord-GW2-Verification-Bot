# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM openjdk:8

ENV VERSION 1.0.4

ADD https://github.com/vennekilde/Discord-GW2-Verification-Bot/releases/download/${VERSION}/Discord-GW2-Verification-Bot-${VERSION}-jar-with-dependencies.jar /app/Discord-GW2-Verification-Bot.jar

COPY discordbot-entrypoint.sh /app/discordbot-entrypoint.sh

RUN chmod +x /app/discordbot-entrypoint.sh
WORKDIR /app
ENTRYPOINT ["/app/discordbot-entrypoint.sh"]
