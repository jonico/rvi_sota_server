/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling._
import cats.data.Xor
import eu.timepit.refined.Refined
import io.circe._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.resolver.db.PackageFilters._
import org.genivi.sota.resolver.types.Package.Metadata
import org.genivi.sota.resolver.types.{Package, Filter, PackageFilter}
import org.genivi.sota.rest.SotaError._


class PackageFilterResourceWordSpec extends ResourceWordSpec {

  "Package filter resource" should {

    val pkgName    = "package"
    val pkgVersion = "1.0.0"
    val filterName = "filter"
    val filterExpr = s"""vin_matches "^X.*""""
    val pkgFilter  =  PackageFilter(Refined(pkgName), Refined(pkgVersion), Refined(filterName))

    "be able to assign exisiting filters to existing packages" in {
      addPackageOK(pkgName, pkgVersion, None, None)
      addFilterOK(filterName, filterExpr)
      addPackageFilterOK(pkgName, pkgVersion, filterName)
    }

    "not allow assignment of filters to non-existing package names" in {
      addPackageFilter("nonexistant", pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        errorCode(responseAs[Json]) shouldBe Xor.Right("package_not_found")
      }
    }

    "not allow assignment of filters to non-existing package versions" in {
      addPackageFilter(pkgName, "0.0.9", filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        errorCode(responseAs[Json]) shouldBe Xor.Right("package_not_found")
      }
    }

    "not allow assignment of non-existing filters to existing packages " in {
      addPackageFilter(pkgName, pkgVersion, "nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        errorCode(responseAs[Json]) shouldBe Xor.Right("filter_not_found")
      }
    }

    "list existing package filters on GET requests" in {
      listPackageFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List(pkgFilter)
      }
    }

    "list packages associated to a filter on GET requests to /packageFilters?filter=:filterName" in {
      listPackagesForFilter(filterName) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[Package]] shouldBe List(Package(Package.Id(Refined(pkgName), Refined(pkgVersion)), None, None))
      }
    }

    "fail to list packages associated to empty filter names" in {
      listPackagesForFilter("") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail to list packages associated to non-existant filters" in {
      listPackagesForFilter("nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        errorCode(responseAs[Json]) shouldBe Xor.Right("filter_not_found")
      }
    }

    "list filters associated to a package on GET requests to /packageFilters?package=:packageName-:packageVersion" in {
      listFiltersForPackage(pkgName, pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(Filter(Refined(filterName), Refined(filterExpr)))
      }
    }

    "fail to list filters associated to a package if no package name is given" in {
      listFiltersForPackage("", pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail to list filters associated to a package if a non-existant package name is given" in {
      listFiltersForPackage("nonexistant", pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "fail to list filters associated to a package if no package version is given" in {
      listFiltersForPackage(pkgName, "") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "fail to list filters associated to a package if a non-existant package version is given" in {
      listFiltersForPackage(pkgName, "6.6.6") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "delete package filters on DELETE requests" in {
      deletePackageFilter(pkgName, pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.OK
        listPackageFilters ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[PackageFilter]] shouldBe List()
        }
      }
    }

    "fail if package filter does not exist" in {
      deletePackageFilter("nonexistant", pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        errorCode(responseAs[Json]) shouldBe Xor.Right("filter_not_found")
      }
    }

    "delete all package filters when a filter is deleted" in {
      addPackageFilterOK(pkgName, pkgVersion, filterName)
      deleteFilterOK(filterName)
      listPackageFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List()
      }
    }


  }
}
