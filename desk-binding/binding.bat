
@echo off
chcp 65001

echo 打包前台资源
cd "./desk-top"
call npm run build
cd ..

echo 删除旧静态资源
rmdir /S /Q "./desk-leg/src/main/resources/static/"
echo 复制静态资源
robocopy /E "./desk-top/dist/" "./desk-leg/src/main/resources/static/"

echo 打包后台资源
cd "./desk-leg"
call mvnw package

echo 万物归一
