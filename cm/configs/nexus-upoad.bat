
if not exist ".\nexus.conf" (
    REM ##### Create file: .nexus.conf
    (
        echo "[nexus]"
        echo "host = nexus01.trustwave.com"
        echo "username = deployment"
        echo "password = deployment123"
    ) >>  ".\nexus.conf"
)

REM ##### Download Nexus upload script
curl -O https://git.trustwave.com/devops/scripts/raw/master/nexus-upload.py

REM ##### Uploading to https://nexus01.trustwave.com/content/groups/public/$group/$artifact/$version/$fnN
set nexus_artifact_id_inst = nepa-bootstrapper
set nexus_artifact_id_upd = nepa-updater
set group_id = com.trustwave.waf

nexus-upload.py -c .\nexus.conf -a %nexus_artifact_id_inst% -v $env:FULL_PRODUCT_VERSION -g %group_id% $env:WORK_AREA\$env:Platform\$env:Configuration\Installer\*Bootstrapper.exe
nexus-upload.py -c .\nexus.conf -a %nexus_artifact_id_upd%  -v $env:FULL_PRODUCT_VERSION -g %group_id% $env:WORK_AREA\$env:Platform\$env:Configuration\Installer\$env:Culture\NEP-$env:Configuration-$env:Platform-Updater.msi




