package skills.service.controller

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import skills.service.controller.exceptions.InvalidContentTypeException
import skills.service.controller.exceptions.MaxIconSizeExceeded
import skills.service.icons.CustomIconFacade
import skills.service.icons.UploadedIcon
import skills.service.profile.EnableCallStackProf

import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/admin")
@Slf4j
@EnableCallStackProf
class CustomIconAdminController {
    private static final long maxIconFileSize = 1024*1024

    @Autowired
    CustomIconFacade iconFacade

    @RequestMapping(value = "/projects/{projectId}/icons/upload", method = [RequestMethod.PUT, RequestMethod.POST], produces = "application/json")
    @ResponseBody
    UploadedIcon addCustomIcon(
            @PathVariable("projectId") String projectId,
            @RequestParam("customIcon") MultipartFile icon) {
        String iconFilename = icon.originalFilename
        byte[] file = icon.bytes
        icon.contentType

        if (!icon.contentType?.toLowerCase()?.startsWith("image/")) {
            throw new InvalidContentTypeException("content-type [${icon.contentType}] is unacceptable, only image/ content-types are allowed")
        }

        if (file.length > maxIconFileSize) {
            throw new MaxIconSizeExceeded("[${file.length}] exceeds the maximum icon size of [${FileUtils.byteCountToDisplaySize(maxIconFileSize)}]")
        }

        UploadedIcon result = iconFacade.saveIcon(projectId, iconFilename, icon.contentType, file)

        return result
    }

    @RequestMapping(value = "/projects/{projectId}/icons/{filename}", method = RequestMethod.DELETE)
    ResponseEntity<Boolean> delete(@PathVariable("projectId") String projectId, @PathVariable("filename") String filename) {
        iconFacade.deleteIcon(projectId, filename)
        return ResponseEntity.ok(true)
    }
}
