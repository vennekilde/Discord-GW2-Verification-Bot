# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM openjdk:8

ADD target/Discord-GW2-Verification-Bot-jar-with-dependencies.jar /app/Discord-GW2-Verification-Bot.jar

COPY docker/discordbot-entrypoint.sh /app/discordbot-entrypoint.sh

RUN chmod +x /app/discordbot-entrypoint.sh
WORKDIR /app
ENTRYPOINT ["/app/discordbot-entrypoint.sh"]
