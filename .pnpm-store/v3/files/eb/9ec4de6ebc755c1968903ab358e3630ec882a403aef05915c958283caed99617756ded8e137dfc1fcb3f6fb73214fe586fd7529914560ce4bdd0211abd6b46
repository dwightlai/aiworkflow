@SETLOCAL
@IF EXIST "%~dp0\node.exe" (
  "%~dp0\node.exe"  "%~dp0\..\..\..\..\..\..\uuid@9.0.1\node_modules\uuid\dist\bin\uuid" %*
) ELSE (
  @SET PATHEXT=%PATHEXT:;.JS;=;%
  node  "%~dp0\..\..\..\..\..\..\uuid@9.0.1\node_modules\uuid\dist\bin\uuid" %*
)
